package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnection;
import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.ProxyRoom;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.exception.SessionAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.PlayerConnectionNotFoundException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.ProxyRoomAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.ProxyRoomNotFoundException;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.Session;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class VirtualizerRuntime implements PlayerConnectionLifecycle {

  private final Map<RoomId, ProxyRoom> roomsById = new LinkedHashMap<>();
  private final Map<PlayerId, PlayerConnection> connectionsByPlayer = new LinkedHashMap<>();
  private final Map<PlayerId, RoomId> sessionRoomIdsByPlayer = new LinkedHashMap<>();

  public void registerRoom(@NotNull ProxyRoom room) {
    Objects.requireNonNull(room, "room");
    if (roomsById.putIfAbsent(room.id(), room) != null)
      throw new ProxyRoomAlreadyExistsException(room.id());
  }

  @NotNull
  public List<ProxyRoom> rooms() {
    return List.copyOf(roomsById.values());
  }

  @NotNull
  public Optional<ProxyRoom> findRoom(@NotNull RoomId roomId) {
    Objects.requireNonNull(roomId, "roomId");
    return Optional.ofNullable(roomsById.get(roomId));
  }

  @NotNull
  public Optional<PlayerConnection> findConnection(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return Optional.ofNullable(connectionsByPlayer.get(playerId));
  }

  @NotNull
  public Session openSession(
      @NotNull PlayerId playerId, @NotNull RoomId roomId, @NotNull Clock clock) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(roomId, "roomId");
    Objects.requireNonNull(clock, "clock");

    if (sessionRoomIdsByPlayer.containsKey(playerId))
      throw new SessionAlreadyExistsException("Player already has an open session: " + playerId);

    PlayerConnection connection = requireConnection(playerId);
    ProxyRoom room = requireRoom(roomId);
    Session session = room.openSession(playerId, connection.id(), clock);
    sessionRoomIdsByPlayer.put(playerId, roomId);
    return session;
  }

  @NotNull
  public Optional<Session> findSession(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    RoomId roomId = sessionRoomIdsByPlayer.get(playerId);
    if (roomId == null) return Optional.empty();

    return requireRoom(roomId).findSession(playerId);
  }

  @NotNull
  public Optional<Session> closeSession(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    RoomId roomId = sessionRoomIdsByPlayer.remove(playerId);
    if (roomId == null) return Optional.empty();

    return requireRoom(roomId).closeSession(playerId);
  }

  @Override
  public void playerConnected(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    connectionsByPlayer.putIfAbsent(playerId, PlayerConnection.open(playerId));
  }

  @Override
  public void playerDisconnected(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    closeSession(playerId);
    connectionsByPlayer.remove(playerId);
  }

  private PlayerConnection requireConnection(PlayerId playerId) {
    PlayerConnection connection = connectionsByPlayer.get(playerId);
    if (connection == null) throw new PlayerConnectionNotFoundException(playerId);

    return connection;
  }

  private ProxyRoom requireRoom(RoomId roomId) {
    ProxyRoom room = roomsById.get(roomId);
    if (room == null) throw new ProxyRoomNotFoundException(roomId);

    return room;
  }
}
