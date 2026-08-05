package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.lang.reflect.Proxy;
import java.util.UUID;
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
    assertThat(recordingLifecycle.disconnectedPlayerId).isNull();
  }

  @Test
  void forwardsDisconnectedPlayerIdentityToCore() {
    RecordingLifecycle recordingLifecycle = new RecordingLifecycle();
    VelocityPlayerConnectionLifecycle lifecycle =
        new VelocityPlayerConnectionLifecycle(recordingLifecycle);
    UUID playerUniqueId = UUID.randomUUID();

    lifecycle.playerDisconnected(player(playerUniqueId));

    assertThat(recordingLifecycle.disconnectedPlayerId).isEqualTo(new PlayerId(playerUniqueId));
    assertThat(recordingLifecycle.connectedPlayerId).isNull();
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
    private PlayerId disconnectedPlayerId;

    @Override
    public void playerConnected(PlayerId playerId) {
      connectedPlayerId = playerId;
    }

    @Override
    public void playerDisconnected(PlayerId playerId) {
      disconnectedPlayerId = playerId;
    }
  }
}
