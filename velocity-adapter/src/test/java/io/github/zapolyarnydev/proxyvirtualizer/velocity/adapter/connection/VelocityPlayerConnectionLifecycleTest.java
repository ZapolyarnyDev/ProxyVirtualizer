package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class VelocityPlayerConnectionLifecycleTest {

  @Test
  void forwardsConnectedPlayerIdentityToCore() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle, new VelocityConnectionRegistry());
    UUID playerUniqueId = UUID.randomUUID();

    lifecycle.playerConnected(player(playerUniqueId));

    assertThat(recordingLifecycle.connectedPlayerId).isEqualTo(new PlayerId(playerUniqueId));
    assertThat(recordingLifecycle.connectedConnectionId).isNotNull();
  }

  @Test
  void forwardsDisconnectedPlayerIdentityToCore() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle, new VelocityConnectionRegistry());
    UUID playerUniqueId = UUID.randomUUID();

    Player player = player(playerUniqueId);
    lifecycle.playerConnected(player);
    lifecycle.playerDisconnected(player);

    assertThat(recordingLifecycle.disconnectedConnectionId)
        .isEqualTo(recordingLifecycle.connectedConnectionId);
  }

  @Test
  void assignsDistinctConnectionIdentitiesToSeparateVelocityConnections() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle, new VelocityConnectionRegistry());
    UUID playerUniqueId = UUID.randomUUID();

    lifecycle.playerConnected(player(playerUniqueId));
    ConnectionId firstConnectionId = recordingLifecycle.connectedConnectionId;
    lifecycle.playerConnected(player(playerUniqueId));

    assertThat(recordingLifecycle.connectedConnectionId).isNotEqualTo(firstConnectionId);
  }

  @Test
  void rollsBackAdapterRegistrationWhenCoreConnectFails() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    recordingLifecycle.connectResult =
        CompletableFuture.failedFuture(new IllegalStateException("connect failed"));
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle, registry);
    Player player = player(UUID.randomUUID());

    lifecycle.playerConnected(player).handle((ignored, cause) -> null).toCompletableFuture().join();

    assertThat(registry.findConnection(player)).isEmpty();
  }

  @Test
  void failedRepeatedConnectPreservesExistingRegistration() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    Player player = player(UUID.randomUUID());
    VelocityConnection existing = registry.register(player).connection();
    recordingLifecycle.connectResult =
        CompletableFuture.failedFuture(new IllegalStateException("connect failed"));
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle, registry);

    lifecycle.playerConnected(player).handle((ignored, cause) -> null).toCompletableFuture().join();

    assertThat(registry.findConnection(player)).contains(existing);
  }

  @Test
  void unregistersAdapterConnectionOnlyAfterCoreDisconnectSucceeds() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    CompletableFuture<Void> disconnectResult = new CompletableFuture<>();
    recordingLifecycle.disconnectResult = disconnectResult;
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle, registry);
    Player player = player(UUID.randomUUID());
    lifecycle.playerConnected(player);

    CompletionStage<Void> disconnecting = lifecycle.playerDisconnected(player);
    assertThat(registry.findConnection(player)).isPresent();

    disconnectResult.complete(null);
    disconnecting.toCompletableFuture().join();
    assertThat(registry.findConnection(player)).isEmpty();
  }

  @Test
  void preservesAdapterConnectionWhenCoreDisconnectFails() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    recordingLifecycle.disconnectResult =
        CompletableFuture.failedFuture(new IllegalStateException("disconnect failed"));
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle, registry);
    Player player = player(UUID.randomUUID());
    lifecycle.playerConnected(player);

    lifecycle
        .playerDisconnected(player)
        .handle((ignored, cause) -> null)
        .toCompletableFuture()
        .join();

    assertThat(registry.findConnection(player)).isPresent();
  }

  @Test
  void staleDisconnectCannotRemoveReplacementRegistration() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    CompletableFuture<Void> disconnectResult = new CompletableFuture<>();
    recordingLifecycle.disconnectResult = disconnectResult;
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle, registry);
    Player player = player(UUID.randomUUID());
    lifecycle.playerConnected(player);
    CompletionStage<Void> disconnecting = lifecycle.playerDisconnected(player);
    registry.unregister(player);
    VelocityConnection replacement = registry.register(player).connection();

    disconnectResult.complete(null);
    disconnecting.toCompletableFuture().join();

    assertThat(registry.findConnection(player)).contains(replacement);
  }

  private static Player player(UUID uniqueId) {
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("getUniqueId")) return uniqueId;

              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static final class RecordingLifecycle implements PlayerConnectionLifecycle {

    private PlayerId connectedPlayerId;
    private ConnectionId connectedConnectionId;
    private ConnectionId disconnectedConnectionId;
    private CompletionStage<Void> connectResult = CompletableFuture.completedFuture(null);
    private CompletionStage<Void> disconnectResult = CompletableFuture.completedFuture(null);

    @Override
    public CompletionStage<Void> playerConnected(PlayerId playerId, ConnectionId connectionId) {
      connectedPlayerId = playerId;
      connectedConnectionId = connectionId;
      return connectResult;
    }

    @Override
    public CompletionStage<Void> playerDisconnected(ConnectionId connectionId) {
      disconnectedConnectionId = connectionId;
      return disconnectResult;
    }
  }
}
