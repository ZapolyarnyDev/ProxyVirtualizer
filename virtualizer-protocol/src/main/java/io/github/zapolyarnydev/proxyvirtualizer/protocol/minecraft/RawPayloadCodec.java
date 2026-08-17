package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolPacket;
import java.nio.ByteBuffer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

final class RawPayloadCodec<P extends ProtocolPacket> implements PacketCodec<P> {
  private final Class<P> type;
  private final Function<byte[], P> factory;
  private final Function<P, byte[]> payload;

  RawPayloadCodec(Class<P> type, Function<byte[], P> factory, Function<P, byte[]> payload) {
    this.type = type;
    this.factory = factory;
    this.payload = payload;
  }

  @Override
  public @NotNull Class<P> packetType() {
    return type;
  }

  @Override
  public @NotNull P decode(@NotNull ByteBuffer input) {
    byte[] bytes = new byte[input.remaining()];
    input.get(bytes);
    return factory.apply(bytes);
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull P packet) {
    return ByteBuffer.wrap(payload.apply(packet));
  }
}
