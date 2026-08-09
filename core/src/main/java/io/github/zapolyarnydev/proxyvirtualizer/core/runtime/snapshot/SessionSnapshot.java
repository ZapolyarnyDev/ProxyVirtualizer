package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.Session;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.SessionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.SessionState;
import java.time.Instant;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record SessionSnapshot(
    @NotNull SessionId id,
    @NotNull PlayerId playerId,
    @NotNull ConnectionId connectionId,
    @NotNull SessionState state,
    @NotNull Instant createdAt) {

  public SessionSnapshot {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(connectionId, "connectionId");
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(createdAt, "createdAt");
  }

  @NotNull
  public static SessionSnapshot from(Session session) {
    return new SessionSnapshot(
        session.id(),
        session.playerId(),
        session.connectionId(),
        session.state(),
        session.createdAt());
  }
}
