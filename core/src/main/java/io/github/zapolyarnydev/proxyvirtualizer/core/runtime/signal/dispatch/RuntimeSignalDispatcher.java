package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.dispatch;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RuntimeSignal;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface RuntimeSignalDispatcher {

  void dispatch(@NotNull RuntimeSignal signal);

  @NotNull
  static RuntimeSignalDispatcher noop() {
    return signal -> {};
  }

  @NotNull
  static RuntimeSignalDispatcher async(
      @NotNull Executor executor, @NotNull RuntimeSignalListener listener) {
    Objects.requireNonNull(executor, "executor");
    Objects.requireNonNull(listener, "listener");
    return signal -> executor.execute(() -> listener.onSignal(signal));
  }
}
