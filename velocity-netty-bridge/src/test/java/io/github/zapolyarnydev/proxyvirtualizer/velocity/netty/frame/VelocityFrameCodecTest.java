package io.github.zapolyarnydev.proxyvirtualizer.velocity.netty.frame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.frame.OutboundFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

final class VelocityFrameCodecTest {

  @Test
  void decodesPacketIdAndPayloadWithoutChangingInputReaderIndex() {
    ByteBuf input = Unpooled.buffer();
    input.writeByte(0xAC);
    input.writeByte(0x02);
    input.writeBytes(new byte[] {1, 2, 3});
    int readerIndex = input.readerIndex();

    var frame = VelocityFrameCodec.decode(input);

    assertThat(frame.packetId()).isEqualTo(new PacketId(300));
    ByteBuffer payload = frame.payload();
    byte[] payloadBytes = new byte[payload.remaining()];
    payload.get(payloadBytes);
    assertThat(payloadBytes).containsExactly((byte) 1, (byte) 2, (byte) 3);
    assertThat(input.readerIndex()).isEqualTo(readerIndex);
    input.release();
  }

  @Test
  void encodesPacketIdBeforePayload() {
    ByteBuf encoded =
        VelocityFrameCodec.encode(
            UnpooledByteBufAllocatorHolder.INSTANCE,
            new OutboundFrame(new PacketId(300), ByteBuffer.wrap(new byte[] {1, 2, 3})));

    assertThat(encoded.readUnsignedByte()).isEqualTo((short) 0xAC);
    assertThat(encoded.readUnsignedByte()).isEqualTo((short) 0x02);
    assertThat(encoded.readByte()).isEqualTo((byte) 1);
    assertThat(encoded.readByte()).isEqualTo((byte) 2);
    assertThat(encoded.readByte()).isEqualTo((byte) 3);
    encoded.release();
  }

  @Test
  void rejectsIncompletePacketId() {
    ByteBuf input = Unpooled.wrappedBuffer(new byte[] {(byte) 0x80});

    assertThatIllegalArgumentException().isThrownBy(() -> VelocityFrameCodec.decode(input));
    input.release();
  }

  private static final class UnpooledByteBufAllocatorHolder {

    private static final io.netty.buffer.ByteBufAllocator INSTANCE =
        UnpooledByteBufAllocator.DEFAULT;
  }
}
