package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class VelocityPlayerConnectionListener {

  private final VelocityPlayerConnectionLifecycle lifecycle;

  public VelocityPlayerConnectionListener(@NotNull VelocityPlayerConnectionLifecycle lifecycle) {
    this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
  }

  @Subscribe
  public void onPostLogin(PostLoginEvent event) {
    lifecycle.playerConnected(event.getPlayer());
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    lifecycle.playerDisconnected(event.getPlayer());
  }
}
