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
    VelocityConnectionRegistration registration = connections.register(player);
    VelocityConnection connection = registration.connection();
    CompletionStage<Void> result;
    try {
      result = lifecycle.playerConnected(connection.playerId(), connection.connectionId());
    } catch (RuntimeException cause) {
      rollback(registration);
      throw cause;
    }
    return result.whenComplete(
        (ignored, cause) -> {
          if (cause != null) rollback(registration);
        });
  }

  @NotNull
  public CompletionStage<Void> playerDisconnected(@NotNull Player player) {
    return connections
        .findConnection(player)
        .map(
            connection ->
                lifecycle
                    .playerDisconnected(connection.connectionId())
                    .thenRun(() -> connections.unregister(connection)))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  private void rollback(VelocityConnectionRegistration registration) {
    if (registration.created()) connections.unregister(registration.connection());
  }
}
