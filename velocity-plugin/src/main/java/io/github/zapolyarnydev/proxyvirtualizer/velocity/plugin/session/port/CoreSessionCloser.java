package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.port;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CoreSessionCloser {

  @NotNull
  CompletionStage<?> closeSession(@NotNull PlayerId playerId);
}
