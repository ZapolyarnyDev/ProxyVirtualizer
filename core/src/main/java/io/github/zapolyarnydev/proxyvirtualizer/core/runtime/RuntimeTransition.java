package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RuntimeSignal;
import java.util.List;
import java.util.Objects;

record RuntimeTransition<T>(T value, List<RuntimeSignal> signals) {

  RuntimeTransition {
    Objects.requireNonNull(value, "value");
    signals = List.copyOf(signals);
  }

  static <T> RuntimeTransition<T> withoutSignals(T value) {
    return new RuntimeTransition<>(value, List.of());
  }
}
