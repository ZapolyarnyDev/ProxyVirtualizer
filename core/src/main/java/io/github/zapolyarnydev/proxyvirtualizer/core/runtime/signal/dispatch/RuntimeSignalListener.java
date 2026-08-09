package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.dispatch;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RuntimeSignal;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface RuntimeSignalListener {

  void onSignal(@NotNull RuntimeSignal signal);
}
