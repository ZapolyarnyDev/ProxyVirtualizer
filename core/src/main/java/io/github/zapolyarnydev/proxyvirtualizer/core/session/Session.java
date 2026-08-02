package io.github.zapolyarnydev.proxyvirtualizer.core.session;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.exception.SessionStateException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record Session(
    @NotNull SessionId id,
    @NotNull PlayerId playerId,
    @NotNull ConnectionId connectionId,
    @NotNull SessionState state,
    @NotNull Instant createdAt) {

  public Session {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(connectionId, "connectionId");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(createdAt, "createdAt");
  }

  public static Session create(
      @NotNull PlayerId playerId, @NotNull ConnectionId connectionId, @NotNull Clock clock) {
    Objects.requireNonNull(clock, "clock");
    return new Session(
        SessionId.random(), playerId, connectionId, SessionState.INITIALIZING, clock.instant());
  }

  @NotNull
  public Session activate() {
    if (state != SessionState.INITIALIZING)
      throw new SessionStateException("Only an initializing session can become active");

    return withState(SessionState.ACTIVE);
  }

  @NotNull
  public Session close() {
    if (state == SessionState.CLOSED)
      throw new SessionStateException("A closed session cannot be closed again");

    return withState(SessionState.CLOSED);
  }

  @NotNull
  private Session withState(SessionState newState) {
    return new Session(id, playerId, connectionId, newState, createdAt);
  }
}
