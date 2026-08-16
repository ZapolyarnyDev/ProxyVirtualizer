package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class VelocityBackendConnections {

  private VelocityBackendConnections() {}

  static VelocityBackendConnection suspend(Player player) {
    Objects.requireNonNull(player, "player");
    Optional<RegisteredServer> target = player.getCurrentServer().map(ServerConnection::getServer);
    if (target.isEmpty()) return new DetachedBackendConnection(player, null);

    Object connectedServer = invoke(player, "getConnectedServer");
    if (connectedServer == null)
      throw new IllegalStateException(
          "Velocity player has a current server without a server connection");

    Object backendConnection = invoke(connectedServer, "getConnection");
    if (backendConnection == null)
      throw new IllegalStateException(
          "Velocity server connection does not expose a backend channel");

    Object backendChannel = invoke(backendConnection, "getChannel");
    invokeWithNull(player, "setConnectedServer");
    invoke(backendChannel, "close");
    return new DetachedBackendConnection(player, target.get());
  }

  private static Object invoke(Object target, String methodName) {
    try {
      return target.getClass().getMethod(methodName).invoke(target);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new IllegalStateException(
          "Velocity connection does not support " + methodName + "()", exception);
    }
  }

  private static void invokeWithNull(Object target, String methodName) {
    Method method =
        java.util.Arrays.stream(target.getClass().getMethods())
            .filter(candidate -> candidate.getName().equals(methodName))
            .filter(candidate -> candidate.getParameterCount() == 1)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Velocity connection does not support " + methodName + "(null)"));
    try {
      method.invoke(target, new Object[] {null});
    } catch (IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalStateException(
          "Velocity connection rejected " + methodName + "(null)", exception);
    }
  }

  private record DetachedBackendConnection(Player player, RegisteredServer target)
      implements VelocityBackendConnection {

    @Override
    public CompletionStage<Void> restore() {
      if (target == null) return CompletableFuture.completedFuture(null);
      if (!isClientChannelActive(player))
        return CompletableFuture.failedFuture(
            new IllegalStateException("Velocity client channel is no longer active"));
      if (player.getCurrentServer().isPresent())
        return CompletableFuture.failedFuture(
            new IllegalStateException("Velocity player is already connected to a backend server"));

      return player
          .createConnectionRequest(target)
          .connect()
          .thenCompose(
              result ->
                  result.isSuccessful()
                      ? CompletableFuture.completedFuture(null)
                      : CompletableFuture.failedFuture(
                          new IllegalStateException(
                              "Could not restore backend connection: " + result.getStatus())));
    }

    @Override
    public void terminate() {
      try {
        Object connection = invoke(player, "getConnection");
        Object channel = invoke(connection, "getChannel");
        invoke(channel, "close");
      } catch (RuntimeException ignored) {
      }
    }

    private static boolean isClientChannelActive(Player player) {
      Object connection = invoke(player, "getConnection");
      Object channel = invoke(connection, "getChannel");
      Object active = invoke(channel, "isActive");
      if (active instanceof Boolean value) return value;
      throw new IllegalStateException("Velocity client channel does not expose an active state");
    }
  }
}
