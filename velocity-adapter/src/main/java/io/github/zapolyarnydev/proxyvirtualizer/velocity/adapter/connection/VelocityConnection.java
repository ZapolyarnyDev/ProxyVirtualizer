package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record VelocityConnection(
    @NotNull Player player, @NotNull PlayerId playerId, @NotNull ConnectionId connectionId) {

  public VelocityConnection {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(connectionId, "connectionId");
  }
}
