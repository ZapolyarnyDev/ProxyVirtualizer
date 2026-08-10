package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.frame.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.MalformedPacketException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.UnknownPacketCodecException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft.Minecraft26_2Protocol;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft.packet.ServerboundKeepAlivePacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class InboundPacketDecoderTest {

  private InboundPacketDecoder decoder;

  @BeforeEach
  void setUp() {
    ProtocolRegistry registry = new ProtocolRegistry();
    Minecraft26_2Protocol.install(registry);
    decoder = new InboundPacketDecoder(registry);
  }

  @Test
  void decodesMinecraft26_2ServerboundKeepAlive() {
    long keepAliveId = -8_192_837_465_019L;
    InboundFrame frame =
        new InboundFrame(
            Minecraft26_2Protocol.SERVERBOUND_KEEP_ALIVE_ID,
            ByteBuffer.allocate(Long.BYTES).putLong(keepAliveId).flip());

    assertThat(decoder.decode(Minecraft26_2Protocol.VERSION, ProtocolPhase.PLAY, frame))
        .isEqualTo(new ServerboundKeepAlivePacket(keepAliveId));
  }

  @Test
  void rejectsUnknownPacketId() {
    InboundFrame frame = new InboundFrame(new PacketId(0x7F), ByteBuffer.allocate(0));

    assertThatThrownBy(
            () -> decoder.decode(Minecraft26_2Protocol.VERSION, ProtocolPhase.PLAY, frame))
        .isInstanceOf(UnknownPacketCodecException.class)
        .hasMessageContaining("packet 127");
  }

  @Test
  void rejectsTruncatedPayload() {
    InboundFrame frame =
        new InboundFrame(
            Minecraft26_2Protocol.SERVERBOUND_KEEP_ALIVE_ID,
            ByteBuffer.allocate(Integer.BYTES).putInt(42).flip());

    assertThatThrownBy(
            () -> decoder.decode(Minecraft26_2Protocol.VERSION, ProtocolPhase.PLAY, frame))
        .isInstanceOf(MalformedPacketException.class)
        .hasMessageContaining("could not decode payload");
  }

  @Test
  void rejectsTrailingPayload() {
    InboundFrame frame =
        new InboundFrame(
            Minecraft26_2Protocol.SERVERBOUND_KEEP_ALIVE_ID,
            ByteBuffer.allocate(Long.BYTES + 1).putLong(42L).put((byte) 1).flip());

    assertThatThrownBy(
            () -> decoder.decode(Minecraft26_2Protocol.VERSION, ProtocolPhase.PLAY, frame))
        .isInstanceOf(MalformedPacketException.class)
        .hasMessageContaining("1 unread payload byte");
  }
}
