package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

public final class VelocityPlayerConnectionLifecycle {

  private final PlayerConnectionLifecycle lifecycle;
  private final ConcurrentMap<UUID, ConnectionId> connectionIdsByPlayer = new ConcurrentHashMap<>();

  public VelocityPlayerConnectionLifecycle(@NotNull PlayerConnectionLifecycle lifecycle) {
    this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
  }

  @NotNull
  public CompletionStage<Void> playerConnected(@NotNull Player player) {
    PlayerId playerId = playerId(player);
    ConnectionId connectionId =
        connectionIdsByPlayer.computeIfAbsent(playerId.id(), ignored -> ConnectionId.random());
    return lifecycle.playerConnected(playerId, connectionId);
  }

  @NotNull
  public CompletionStage<Void> playerDisconnected(@NotNull Player player) {
    ConnectionId connectionId = connectionIdsByPlayer.remove(playerId(player).id());
    if (connectionId == null) return CompletableFuture.completedFuture(null);

    return lifecycle.playerDisconnected(connectionId);
  }

  private static PlayerId playerId(Player player) {
    Objects.requireNonNull(player, "player");
    return new PlayerId(player.getUniqueId());
  }
}
