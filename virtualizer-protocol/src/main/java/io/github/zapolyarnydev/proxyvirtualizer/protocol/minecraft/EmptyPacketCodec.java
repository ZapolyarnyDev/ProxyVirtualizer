package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolPacket;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

final class EmptyPacketCodec<P extends ProtocolPacket> implements PacketCodec<P> {

  private final Class<P> packetType;
  private final Supplier<P> factory;

  EmptyPacketCodec(Class<P> packetType, Supplier<P> factory) {
    this.packetType = Objects.requireNonNull(packetType, "packetType");
    this.factory = Objects.requireNonNull(factory, "factory");
  }

  @Override
  public @NotNull Class<P> packetType() {
    return packetType;
  }

  @Override
  public @NotNull P decode(@NotNull ByteBuffer input) {
    if (input.hasRemaining()) throw new IllegalArgumentException("Expected an empty payload");
    return factory.get();
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull P packet) {
    return ByteBuffer.allocate(0);
  }
}
