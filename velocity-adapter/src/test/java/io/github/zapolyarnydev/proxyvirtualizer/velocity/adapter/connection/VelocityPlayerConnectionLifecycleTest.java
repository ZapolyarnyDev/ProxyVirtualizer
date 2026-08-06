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
        new VelocityPlayerConnectionLifecycle(recordingLifecycle);
    UUID playerUniqueId = UUID.randomUUID();

    lifecycle.playerConnected(player(playerUniqueId));

    assertThat(recordingLifecycle.connectedPlayerId).isEqualTo(new PlayerId(playerUniqueId));
    assertThat(recordingLifecycle.connectedConnectionId).isNotNull();
  }

  @Test
  void forwardsDisconnectedPlayerIdentityToCore() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle);
    UUID playerUniqueId = UUID.randomUUID();

    Player player = player(playerUniqueId);
    lifecycle.playerConnected(player);
    lifecycle.playerDisconnected(player);

    assertThat(recordingLifecycle.disconnectedConnectionId)
        .isEqualTo(recordingLifecycle.connectedConnectionId);
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

    @Override
    public CompletionStage<Void> playerConnected(PlayerId playerId, ConnectionId connectionId) {
      connectedPlayerId = playerId;
      connectedConnectionId = connectionId;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> playerDisconnected(ConnectionId connectionId) {
      disconnectedConnectionId = connectionId;
      return CompletableFuture.completedFuture(null);
    }
  }
}
