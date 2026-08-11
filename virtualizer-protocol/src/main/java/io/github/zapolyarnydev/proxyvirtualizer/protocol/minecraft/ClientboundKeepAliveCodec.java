package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public final class ClientboundKeepAliveCodec implements PacketCodec<ClientboundKeepAlivePacket> {

  @Override
  public @NotNull Class<ClientboundKeepAlivePacket> packetType() {
    return ClientboundKeepAlivePacket.class;
  }

  @Override
  public @NotNull ClientboundKeepAlivePacket decode(@NotNull ByteBuffer input) {
    return new ClientboundKeepAlivePacket(input.getLong());
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull ClientboundKeepAlivePacket packet) {
    return ByteBuffer.allocate(Long.BYTES).putLong(packet.id()).flip();
  }
}
