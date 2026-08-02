package io.github.zapolyarnydev.proxyvirtualizer.core.room;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.exception.ProxyRoomFullException;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.exception.SessionAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.exception.SessionNotFoundException;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.Session;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class ProxyRoom {

  private final long id;
  private final Map<PlayerId, Session> sessionsByPlayer = new LinkedHashMap<>();
  private SessionLimit sessionLimit = SessionLimit.unlimited();

  public ProxyRoom(long id) {
    this.id = id;
  }

  public long id() {
    return id;
  }

  public List<Session> sessions() {
    return List.copyOf(sessionsByPlayer.values());
  }

  public boolean isFull() {
    return sessionLimit.isReached(sessionsByPlayer.size());
  }

  public void limitRoomSessions(int maxSessions) {
    if (maxSessions < sessionsByPlayer.size())
      throw new IllegalArgumentException(
          "Session limit cannot be lower than the current occupancy");

    sessionLimit = SessionLimit.limited(maxSessions);
  }

  public void unlimitRoomSessions() {
    sessionLimit = SessionLimit.unlimited();
  }

  public Session openSession(
      @NotNull PlayerId playerId, @NotNull ConnectionId connectionId, @NotNull Clock clock) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(connectionId, "connectionId");
    Objects.requireNonNull(clock, "clock");

    ensurePlayerHasNoSession(playerId);
    ensureConnectionIsFree(connectionId);
    ensureRoomHasCapacity();

    Session session = Session.create(playerId, connectionId, clock);
    sessionsByPlayer.put(playerId, session);
    return session;
  }

  public Session activateSession(@NotNull PlayerId playerId) {
    Session session = requireSession(playerId).activate();
    sessionsByPlayer.put(playerId, session);
    return session;
  }

  public Optional<Session> findSession(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return Optional.ofNullable(sessionsByPlayer.get(playerId));
  }

  public Optional<Session> closeSession(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return Optional.ofNullable(sessionsByPlayer.remove(playerId)).map(Session::close);
  }

  private boolean hasConnection(@NotNull ConnectionId connectionId) {
    return sessionsByPlayer.values().stream()
        .anyMatch(session -> session.connectionId().equals(connectionId));
  }

  private Session requireSession(@NotNull PlayerId playerId) {
    Session session = sessionsByPlayer.get(playerId);
    if (session == null)
      throw new SessionNotFoundException("No session belongs to player: " + playerId);

    return session;
  }

  private void ensurePlayerHasNoSession(PlayerId playerId) {
    if (sessionsByPlayer.containsKey(playerId))
      throw new SessionAlreadyExistsException(
          "Player already has a session in this room: " + playerId);
  }

  private void ensureConnectionIsFree(ConnectionId id) {
    if (hasConnection(id))
      throw new SessionAlreadyExistsException(
          "Connection already has a session in this room: " + id);
  }

  private void ensureRoomHasCapacity() {
    if (isFull()) throw new ProxyRoomFullException(this.id);
  }
}
