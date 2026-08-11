package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.SemanticAction;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.SemanticActionHandler;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.SemanticActionSink;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.UnknownSemanticActionException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class SemanticActionRouter implements SemanticActionSink {

  private final Map<Class<? extends SemanticAction>, SemanticActionHandler<?>> handlers;

  private SemanticActionRouter(
      Map<Class<? extends SemanticAction>, SemanticActionHandler<?>> handlers) {
    this.handlers = Map.copyOf(handlers);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void accept(@NotNull SemanticAction action) {
    Objects.requireNonNull(action, "action");
    SemanticActionHandler<SemanticAction> handler = castHandler(handlers.get(action.getClass()));
    if (handler == null) {
      throw new UnknownSemanticActionException(
          "No semantic action handler for " + action.getClass().getName());
    }
    handler.handle(action);
  }

  @SuppressWarnings("unchecked")
  private static SemanticActionHandler<SemanticAction> castHandler(
      SemanticActionHandler<?> handler) {
    return (SemanticActionHandler<SemanticAction>) handler;
  }

  public static final class Builder {

    private final Map<Class<? extends SemanticAction>, SemanticActionHandler<?>> handlers =
        new HashMap<>();

    private Builder() {}

    public <A extends SemanticAction> Builder route(
        Class<A> actionType, SemanticActionHandler<A> handler) {
      Objects.requireNonNull(actionType, "actionType");
      Objects.requireNonNull(handler, "handler");
      if (handlers.putIfAbsent(actionType, handler) != null) {
        throw new IllegalStateException(
            "Semantic action handler is already registered: " + actionType.getName());
      }
      return this;
    }

    public SemanticActionRouter build() {
      return new SemanticActionRouter(handlers);
    }
  }
}
