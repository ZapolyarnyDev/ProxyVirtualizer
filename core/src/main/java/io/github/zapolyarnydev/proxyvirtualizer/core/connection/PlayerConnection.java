package io.github.zapolyarnydev.proxyvirtualizer.core.connection;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record PlayerConnection(@NotNull PlayerId playerId, @NotNull ConnectionId id) {

  public PlayerConnection {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(id, "id");
  }

  public static PlayerConnection open(@NotNull PlayerId playerId) {
    return new PlayerConnection(playerId, ConnectionId.random());
  }
}
