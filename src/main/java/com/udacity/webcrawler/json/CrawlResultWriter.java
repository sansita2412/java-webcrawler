package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CrawlResult result;

    public CrawlResultWriter(CrawlResult result) {
        this.result = Objects.requireNonNull(result);
    }

    /**
     * Writes JSON to a file path. This method OWNS the writer and may close it.
     */
    public void write(Path path) {
        Objects.requireNonNull(path);

        try (Writer writer =
                     Files.newBufferedWriter(
                             path,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.APPEND)) {

            write(writer);

        } catch (IOException e) {
            throw new RuntimeException("Failed to write crawl result", e);
        }
    }

    /**
     * Writes JSON to an existing writer.
     *
     * <p>IMPORTANT: This method MUST NOT close the writer.
     */
    public void write(Writer writer) {
        Objects.requireNonNull(writer);

        try {
            JsonGenerator generator = OBJECT_MAPPER.getFactory().createGenerator(writer);

            generator.writeStartObject();
            generator.writeObjectFieldStart("wordCounts");
            for (Map.Entry<String, Integer> entry : result.getWordCounts().entrySet()) {
                generator.writeNumberField(entry.getKey(), entry.getValue());
            }
            generator.writeEndObject();

            generator.writeNumberField("urlsVisited", result.getUrlsVisited());



            generator.writeEndObject();

            generator.flush(); // ✅ flush only, DO NOT close

        } catch (IOException e) {
            throw new RuntimeException("Failed to write crawl result", e);
        }
    }
}
