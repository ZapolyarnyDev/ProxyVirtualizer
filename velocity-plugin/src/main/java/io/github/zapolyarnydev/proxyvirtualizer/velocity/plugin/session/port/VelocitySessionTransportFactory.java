package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.port;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface VelocitySessionTransportFactory {

  @NotNull
  SessionTransport create(@NotNull Player player);
}
