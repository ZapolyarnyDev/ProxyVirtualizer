package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnection;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.ProxyRoom;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.exception.SessionAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.PlayerConnectionAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.PlayerConnectionNotFoundException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.ProxyRoomAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.ProxyRoomNotFoundException;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.Session;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class VirtualizerRuntimeState {

  private final Map<RoomId, ProxyRoom> roomsById = new LinkedHashMap<>();
  private final Map<PlayerId, PlayerConnection> connectionsByPlayer = new LinkedHashMap<>();
  private final Map<ConnectionId, PlayerConnection> connectionsById = new LinkedHashMap<>();
  private final Map<PlayerId, RoomId> sessionRoomIdsByPlayer = new LinkedHashMap<>();

  RoomSnapshot registerRoom(RoomId roomId) {
    ProxyRoom room = new ProxyRoom(roomId);
    if (roomsById.putIfAbsent(roomId, room) != null)
      throw new ProxyRoomAlreadyExistsException(roomId);

    return RoomSnapshot.from(room);
  }

  List<RoomSnapshot> rooms() {
    return roomsById.values().stream().map(RoomSnapshot::from).toList();
  }

  Optional<RoomSnapshot> findRoom(RoomId roomId) {
    return Optional.ofNullable(roomsById.get(roomId)).map(RoomSnapshot::from);
  }

  PlayerConnectionSnapshot connect(PlayerId playerId, ConnectionId connectionId) {
    PlayerConnection existingConnection = connectionsByPlayer.get(playerId);
    if (existingConnection != null) {
      if (existingConnection.id().equals(connectionId))
        return PlayerConnectionSnapshot.from(existingConnection);

      throw new PlayerConnectionAlreadyExistsException(playerId, existingConnection.id());
    }

    if (connectionsById.containsKey(connectionId))
      throw new PlayerConnectionAlreadyExistsException(playerId, connectionId);

    PlayerConnection connection = PlayerConnection.open(playerId, connectionId);
    connectionsByPlayer.put(playerId, connection);
    connectionsById.put(connectionId, connection);
    return PlayerConnectionSnapshot.from(connection);
  }

  Optional<PlayerConnectionSnapshot> findConnection(PlayerId playerId) {
    return Optional.ofNullable(connectionsByPlayer.get(playerId))
        .map(PlayerConnectionSnapshot::from);
  }

  SessionSnapshot openSession(PlayerId playerId, RoomId roomId, Clock clock) {
    if (sessionRoomIdsByPlayer.containsKey(playerId))
      throw new SessionAlreadyExistsException("Player already has an open session: " + playerId);

    PlayerConnection connection = requireConnection(playerId);
    ProxyRoom room = requireRoom(roomId);
    Session session = room.openSession(playerId, connection.id(), clock);
    sessionRoomIdsByPlayer.put(playerId, roomId);
    return SessionSnapshot.from(session);
  }

  Optional<SessionSnapshot> findSession(PlayerId playerId) {
    RoomId roomId = sessionRoomIdsByPlayer.get(playerId);
    if (roomId == null) return Optional.empty();

    return requireRoom(roomId).findSession(playerId).map(SessionSnapshot::from);
  }

  Optional<SessionSnapshot> closeSession(PlayerId playerId) {
    RoomId roomId = sessionRoomIdsByPlayer.remove(playerId);
    if (roomId == null) return Optional.empty();

    return requireRoom(roomId).closeSession(playerId).map(SessionSnapshot::from);
  }

  Optional<PlayerConnectionSnapshot> disconnect(ConnectionId connectionId) {
    PlayerConnection connection = connectionsById.remove(connectionId);
    if (connection == null) return Optional.empty();

    connectionsByPlayer.remove(connection.playerId());
    closeSession(connection.playerId());
    return Optional.of(PlayerConnectionSnapshot.from(connection));
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
