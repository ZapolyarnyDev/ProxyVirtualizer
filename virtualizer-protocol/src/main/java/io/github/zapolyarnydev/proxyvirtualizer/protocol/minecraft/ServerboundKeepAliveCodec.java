package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public final class ServerboundKeepAliveCodec implements PacketCodec<ServerboundKeepAlivePacket> {

  @Override
  public @NotNull Class<ServerboundKeepAlivePacket> packetType() {
    return ServerboundKeepAlivePacket.class;
  }

  @Override
  public @NotNull ServerboundKeepAlivePacket decode(@NotNull ByteBuffer input) {
    return new ServerboundKeepAlivePacket(input.getLong());
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull ServerboundKeepAlivePacket packet) {
    return ByteBuffer.allocate(Long.BYTES).putLong(packet.id()).flip();
  }
}
