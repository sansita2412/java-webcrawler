package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

/**
 * A {@link WebCrawler} that fetches and processes pages in parallel using ForkJoinPool.
 */
final class ParallelWebCrawler implements WebCrawler {

    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;
    private final ForkJoinPool pool;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @MaxDepth int maxDepth,
            @IgnoredUrls List<Pattern> ignoredUrls,
            @TargetParallelism int threadCount,
            PageParserFactory parserFactory) {

        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
        this.pool = new ForkJoinPool(threadCount);
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);

        Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        Map<String, Integer> wordCounts = new ConcurrentHashMap<>();

        List<CrawlTask> tasks =
                startingUrls.stream()
                        .map(url ->
                                new CrawlTask(
                                        url,
                                        maxDepth,
                                        deadline,
                                        visitedUrls,
                                        wordCounts))
                        .toList();

        pool.invoke(
                new RecursiveAction() {
                    @Override
                    protected void compute() {
                        invokeAll(tasks);
                    }
                });

        pool.shutdown();


        return new CrawlResult.Builder()
                .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
                .setUrlsVisited(visitedUrls.size())
                .build();
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Recursive Fork/Join task that crawls a single page and spawns subtasks.
     */
    private final class CrawlTask extends RecursiveAction {

        private final String url;
        private final int depth;
        private final Instant deadline;
        private final Set<String> visitedUrls;
        private final Map<String, Integer> wordCounts;

        CrawlTask(
                String url,
                int depth,
                Instant deadline,
                Set<String> visitedUrls,
                Map<String, Integer> wordCounts) {

            this.url = url;
            this.depth = depth;
            this.deadline = deadline;
            this.visitedUrls = visitedUrls;
            this.wordCounts = wordCounts;
        }

        @Override
        protected void compute() {
            // timeout
            if (clock.instant().isAfter(deadline)) {
                return;
            }

            // EXACT legacy depth rule
            if (depth == 0) {
                return;
            }

            // ignored URLs
            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    return;
                }
            }

            // visit once (even if page fails)
            if (!visitedUrls.add(url)) {
                return;
            }

            PageParser.Result result = parserFactory.get(url).parse();

            // merge word counts
            result.getWordCounts().forEach(
                    (word, count) -> wordCounts.merge(word, count, Integer::sum));

            // spawn subtasks
            List<CrawlTask> subtasks =
                    result.getLinks().stream()
                            .map(link ->
                                    new CrawlTask(
                                            link,
                                            depth - 1,
                                            deadline,
                                            visitedUrls,
                                            wordCounts))
                            .toList();

            invokeAll(subtasks);
        }
    }
}
