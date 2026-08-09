package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.dispatch;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RuntimeSignal;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface RuntimeSignalFailureHandler {

  void onFailure(
      @NotNull RuntimeSignalListener listener,
      @NotNull RuntimeSignal signal,
      @NotNull Throwable cause);

  @NotNull
  static RuntimeSignalFailureHandler ignoring() {
    return (listener, signal, cause) -> {};
  }
}
