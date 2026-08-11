package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.OutboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.UnknownPacketCodecException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft.ClientboundKeepAlivePacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft.Minecraft26_2Protocol;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class OutboundPacketEncoderTest {

  private OutboundPacketEncoder encoder;

  @BeforeEach
  void setUp() {
    ProtocolRegistry registry = new ProtocolRegistry();
    Minecraft26_2Protocol.install(registry);
    encoder = new OutboundPacketEncoder(registry);
  }

  @Test
  void encodesMinecraft26_2ClientboundKeepAlive() {
    long id = -6_124_912_481_992L;

    OutboundFrame frame =
        encoder.encode(
            Minecraft26_2Protocol.VERSION, ProtocolPhase.PLAY, new ClientboundKeepAlivePacket(id));

    assertThat(frame.packetId()).isEqualTo(Minecraft26_2Protocol.CLIENTBOUND_KEEP_ALIVE_ID);
    assertThat(frame.payload().remaining()).isEqualTo(Long.BYTES);
    assertThat(frame.payload().getLong()).isEqualTo(id);
  }

  @Test
  void rejectsPacketWithoutClientboundCodec() {
    assertThatThrownBy(
            () ->
                encoder.encode(
                    Minecraft26_2Protocol.VERSION,
                    ProtocolPhase.PLAY,
                    new UnknownClientboundPacket()))
        .isInstanceOf(UnknownPacketCodecException.class)
        .hasMessageContaining(UnknownClientboundPacket.class.getName());
  }

  private record UnknownClientboundPacket() implements ClientboundPacket {}
}
