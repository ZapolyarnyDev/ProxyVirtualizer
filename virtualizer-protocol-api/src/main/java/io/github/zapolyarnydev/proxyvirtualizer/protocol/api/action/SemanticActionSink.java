package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SemanticActionSink {

  void accept(@NotNull SemanticAction action);
}
