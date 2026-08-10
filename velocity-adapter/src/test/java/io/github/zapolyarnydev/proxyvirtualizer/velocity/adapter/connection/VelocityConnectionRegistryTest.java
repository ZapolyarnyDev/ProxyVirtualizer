package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class VelocityConnectionRegistryTest {

  @Test
  void registersPlayerInBothDirections() {
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    UUID playerUniqueId = UUID.randomUUID();
    Player player = player(playerUniqueId);

    VelocityConnection connection = registry.register(player);

    assertThat(connection.player()).isSameAs(player);
    assertThat(connection.playerId()).isEqualTo(new PlayerId(playerUniqueId));
    assertThat(registry.findConnection(player)).contains(connection);
    assertThat(registry.findConnection(connection.connectionId())).contains(connection);
  }

  @Test
  void reusesRegistrationForSameVelocityConnection() {
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    Player player = player(UUID.randomUUID());

    VelocityConnection first = registry.register(player);
    VelocityConnection second = registry.register(player);

    assertThat(second).isSameAs(first);
  }

  @Test
  void assignsDistinctTagsToSeparateConnectionsOfSamePlayer() {
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    UUID playerUniqueId = UUID.randomUUID();

    VelocityConnection first = registry.register(player(playerUniqueId));
    VelocityConnection second = registry.register(player(playerUniqueId));

    assertThat(second.playerId()).isEqualTo(first.playerId());
    assertThat(second.connectionId()).isNotEqualTo(first.connectionId());
  }

  @Test
  void removesBothIndexesOnUnregister() {
    VelocityConnectionRegistry registry = new VelocityConnectionRegistry();
    Player player = player(UUID.randomUUID());
    VelocityConnection connection = registry.register(player);

    assertThat(registry.unregister(player)).contains(connection);

    assertThat(registry.findConnection(player)).isEmpty();
    assertThat(registry.findConnection(connection.connectionId())).isEmpty();
    assertThat(registry.unregister(player)).isEmpty();
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
}
