package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Objects;
import java.time.Duration;
import java.time.Instant;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object target;
  private final ProfilingState profilingState;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Object target, ProfilingState profilingState,Clock clock) {
    this.clock = clock;
    this.target = target;
    this.profilingState = profilingState;

  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
      if (method.getName().equals("equals") && method.getParameterCount() == 1) {
          return target.equals(args[0]);
      }

      boolean profiled = method.isAnnotationPresent(Profiled.class);
      Instant start = null;

      if (profiled) {
          start = clock.instant();
      }

      try {
          Object result = method.invoke(target, args);

          if (profiled) {
              Duration duration = Duration.between(start, clock.instant());
              profilingState.record(target.getClass(), method, duration);
          }

          return result;

      } catch (Throwable t) {
          if (profiled) {
              Duration duration = Duration.between(start, clock.instant());
              profilingState.record(target.getClass(), method, duration);
          }

          throw t.getCause() != null ? t.getCause() : t;

      }

  }
}
