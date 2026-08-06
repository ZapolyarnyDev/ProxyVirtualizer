package io.github.zapolyarnydev.proxyvirtualizer.core.connection;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.NotNull;

public interface PlayerConnectionLifecycle {

  @NotNull
  CompletionStage<Void> playerConnected(
      @NotNull PlayerId playerId, @NotNull ConnectionId connectionId);

  @NotNull
  CompletionStage<Void> playerDisconnected(@NotNull ConnectionId connectionId);
}
