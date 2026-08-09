package io.github.zapolyarnydev.proxyvirtualizer.velocity.netty.transport;

import com.velocitypowered.api.proxy.Player;
import io.netty.channel.Channel;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

final class VelocityChannelAccess {

  private VelocityChannelAccess() {}

  static Channel channel(Player player) {
    Objects.requireNonNull(player, "player");
    try {
      Object connection = player.getClass().getMethod("getConnection").invoke(player);
      Object channel = connection.getClass().getMethod("getChannel").invoke(connection);
      if (channel instanceof Channel nettyChannel) return nettyChannel;

      throw new IllegalStateException("Velocity connection does not expose a Netty channel");
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new IllegalArgumentException(
          "Player is not backed by a supported Velocity connection", exception);
    }
  }
}
