package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.NotNull;

public final class VelocityPlayerConnectionLifecycle {

  private final PlayerConnectionLifecycle lifecycle;
  private final VelocityConnectionRegistry connections;

  public VelocityPlayerConnectionLifecycle(
      @NotNull PlayerConnectionLifecycle lifecycle,
      @NotNull VelocityConnectionRegistry connections) {
    this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    this.connections = Objects.requireNonNull(connections, "connections");
  }

  @NotNull
  public CompletionStage<Void> playerConnected(@NotNull Player player) {
    VelocityConnection connection = connections.register(player);
    return lifecycle.playerConnected(connection.playerId(), connection.connectionId());
  }

  @NotNull
  public CompletionStage<Void> playerDisconnected(@NotNull Player player) {
    return connections
        .unregister(player)
        .map(connection -> lifecycle.playerDisconnected(connection.connectionId()))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }
}
