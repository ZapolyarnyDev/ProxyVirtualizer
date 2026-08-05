package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class VelocityPlayerConnectionLifecycle {

  private final PlayerConnectionLifecycle lifecycle;

  public VelocityPlayerConnectionLifecycle(@NotNull PlayerConnectionLifecycle lifecycle) {
    this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
  }

  public void playerConnected(@NotNull Player player) {
    lifecycle.playerConnected(playerId(player));
  }

  public void playerDisconnected(@NotNull Player player) {
    lifecycle.playerDisconnected(playerId(player));
  }

  private static PlayerId playerId(Player player) {
    Objects.requireNonNull(player, "player");
    return new PlayerId(player.getUniqueId());
  }
}
