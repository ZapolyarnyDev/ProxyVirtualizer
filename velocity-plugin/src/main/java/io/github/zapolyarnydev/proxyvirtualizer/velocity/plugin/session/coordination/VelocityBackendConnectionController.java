package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
interface VelocityBackendConnectionController {

  @NotNull
  VelocityBackendConnection suspend(@NotNull Player player);
}
