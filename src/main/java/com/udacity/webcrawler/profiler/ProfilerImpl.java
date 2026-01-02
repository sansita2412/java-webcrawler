package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ProfilingState profilingState;


    ProfilerImpl(Clock clock) {
        this(clock, new ProfilingState());
    }

    @Inject
    ProfilerImpl(Clock clock, ProfilingState profilingState) {
        this.clock = Objects.requireNonNull(clock);
        this.profilingState = Objects.requireNonNull(profilingState);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T wrap(Class<T> klass, T delegate) {

        boolean hasProfiledMethod =
                java.util.Arrays.stream(klass.getMethods())
                        .anyMatch(m -> m.isAnnotationPresent(Profiled.class));

        if (!hasProfiledMethod) {
            throw new IllegalArgumentException(
                    "Wrapped interface must have at least one @Profiled method");
        }

        return (T)
                Proxy.newProxyInstance(
                        klass.getClassLoader(),
                        new Class<?>[] {klass},
                        new ProfilingMethodInterceptor(delegate, profilingState, clock));
    }

    @Override
    public void writeData(Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            writeData(writer);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeData(Writer writer) {
        try {
            profilingState.write(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
