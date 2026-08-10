package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class VelocityConnectionRegistry {

  private final Map<Player, VelocityConnection> connectionsByPlayer = new IdentityHashMap<>();
  private final Map<ConnectionId, VelocityConnection> connectionsById = new HashMap<>();

  @NotNull
  public synchronized VelocityConnectionRegistration register(@NotNull Player player) {
    Objects.requireNonNull(player, "player");
    VelocityConnection existing = connectionsByPlayer.get(player);
    if (existing != null) return new VelocityConnectionRegistration(existing, false);

    VelocityConnection connection =
        new VelocityConnection(player, new PlayerId(player.getUniqueId()), ConnectionId.random());
    connectionsByPlayer.put(player, connection);
    connectionsById.put(connection.connectionId(), connection);
    return new VelocityConnectionRegistration(connection, true);
  }

  @NotNull
  public synchronized Optional<VelocityConnection> findConnection(@NotNull Player player) {
    Objects.requireNonNull(player, "player");
    return Optional.ofNullable(connectionsByPlayer.get(player));
  }

  @NotNull
  public synchronized Optional<VelocityConnection> findConnection(
      @NotNull ConnectionId connectionId) {
    Objects.requireNonNull(connectionId, "connectionId");
    return Optional.ofNullable(connectionsById.get(connectionId));
  }

  @NotNull
  public synchronized Optional<VelocityConnection> unregister(@NotNull Player player) {
    Objects.requireNonNull(player, "player");
    VelocityConnection connection = connectionsByPlayer.remove(player);
    if (connection == null) return Optional.empty();

    connectionsById.remove(connection.connectionId(), connection);
    return Optional.of(connection);
  }

  @NotNull
  public synchronized Optional<VelocityConnection> unregister(
      @NotNull VelocityConnection connection) {
    Objects.requireNonNull(connection, "connection");
    VelocityConnection registered = connectionsByPlayer.get(connection.player());
    if (registered != connection) return Optional.empty();

    connectionsByPlayer.remove(connection.player());
    connectionsById.remove(connection.connectionId(), connection);
    return Optional.of(connection);
  }
}
