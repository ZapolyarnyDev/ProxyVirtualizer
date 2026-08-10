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
  public void encode(@NotNull ServerboundKeepAlivePacket packet, @NotNull ByteBuffer output) {
    output.putLong(packet.id());
  }
}
