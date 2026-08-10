package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.PacketDirection;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import org.junit.jupiter.api.Test;

final class Minecraft26_2ProtocolTest {

  @Test
  void installsExactProductionProfileAndKeepAliveCodec() {
    ProtocolRegistry registry = new ProtocolRegistry();

    Minecraft26_2Protocol.install(registry);

    assertThat(registry.profiles().require(Minecraft26_2Protocol.VERSION).id())
        .isEqualTo(Minecraft26_2Protocol.PROFILE_ID);
    assertThat(
            registry
                .profiles()
                .require(Minecraft26_2Protocol.VERSION)
                .supports(Minecraft26_2Protocol.INBOUND_KEEP_ALIVE))
        .isTrue();
    assertThat(
            registry
                .codecs()
                .find(
                    Minecraft26_2Protocol.VERSION,
                    ProtocolPhase.PLAY,
                    PacketDirection.SERVERBOUND,
                    Minecraft26_2Protocol.SERVERBOUND_KEEP_ALIVE_ID))
        .isPresent();
  }
}
