package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SemanticActionHandler<A extends SemanticAction> {

  void handle(@NotNull A action);
}
