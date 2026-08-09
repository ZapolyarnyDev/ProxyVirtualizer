package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RuntimeSignal;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

public final class RuntimeSignalBus implements RuntimeSignalDispatcher {

  private final CopyOnWriteArrayList<RuntimeSignalListener> listeners =
      new CopyOnWriteArrayList<>();
  private final RuntimeSignalFailureHandler failureHandler;

  public RuntimeSignalBus() {
    this(RuntimeSignalFailureHandler.ignoring());
  }

  public RuntimeSignalBus(@NotNull RuntimeSignalFailureHandler failureHandler) {
    this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
  }

  public boolean subscribe(@NotNull RuntimeSignalListener listener) {
    Objects.requireNonNull(listener, "listener");
    return listeners.addIfAbsent(listener);
  }

  public boolean unsubscribe(@NotNull RuntimeSignalListener listener) {
    Objects.requireNonNull(listener, "listener");
    return listeners.remove(listener);
  }

  @Override
  public void dispatch(@NotNull RuntimeSignal signal) {
    Objects.requireNonNull(signal, "signal");
    listeners.forEach(listener -> notifyListener(listener, signal));
  }

  private void notifyListener(RuntimeSignalListener listener, RuntimeSignal signal) {
    try {
      listener.onSignal(signal);
    } catch (Throwable cause) {
      notifyFailure(listener, signal, cause);
    }
  }

  private void notifyFailure(
      RuntimeSignalListener listener, RuntimeSignal signal, Throwable cause) {
    try {
      failureHandler.onFailure(listener, signal, cause);
    } catch (Throwable ignored) {
    }
  }
}
