package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot;

import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnection;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record PlayerConnectionSnapshot(
    @NotNull PlayerId playerId, @NotNull ConnectionId connectionId) {

  public PlayerConnectionSnapshot {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(connectionId, "connectionId");
  }

  @NotNull
  public static PlayerConnectionSnapshot from(PlayerConnection connection) {
    return new PlayerConnectionSnapshot(connection.playerId(), connection.id());
  }
}
