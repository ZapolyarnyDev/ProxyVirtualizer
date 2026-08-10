package io.github.zapolyarnydev.proxyvirtualizer.velocity.netty.frame;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.OutboundFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.nio.ByteBuffer;
import java.util.Objects;

public final class VelocityFrameCodec {

  private VelocityFrameCodec() {}

  public static InboundFrame decode(ByteBuf input) {
    Objects.requireNonNull(input, "input");
    ByteBuf frame = input.duplicate();
    int packetId = readVarInt(frame);
    ByteBuffer payload = ByteBuffer.allocate(frame.readableBytes());
    frame.readBytes(payload);
    payload.flip();
    return new InboundFrame(new PacketId(packetId), payload);
  }

  public static ByteBuf encode(ByteBufAllocator allocator, OutboundFrame frame) {
    Objects.requireNonNull(allocator, "allocator");
    Objects.requireNonNull(frame, "frame");
    ByteBuffer payload = frame.payload();
    ByteBuf output = allocator.buffer(varIntSize(frame.packetId().value()) + payload.remaining());
    try {
      writeVarInt(output, frame.packetId().value());
      output.writeBytes(payload);
      return output;
    } catch (Throwable exception) {
      output.release();
      throw exception;
    }
  }

  private static int readVarInt(ByteBuf input) {
    int value = 0;
    for (int position = 0; position < 35; position += 7) {
      if (!input.isReadable()) throw new IllegalArgumentException("Incomplete packet id");

      int current = input.readUnsignedByte();
      value |= (current & 0x7F) << position;
      if ((current & 0x80) == 0) return value;
    }
    throw new IllegalArgumentException("Packet id exceeds VarInt size");
  }

  private static void writeVarInt(ByteBuf output, int value) {
    while ((value & ~0x7F) != 0) {
      output.writeByte((value & 0x7F) | 0x80);
      value >>>= 7;
    }
    output.writeByte(value);
  }

  private static int varIntSize(int value) {
    int size = 1;
    while ((value & ~0x7F) != 0) {
      size++;
      value >>>= 7;
    }
    return size;
  }
}
