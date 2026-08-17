package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public final class RespawnCodec implements PacketCodec<RespawnPacket> {
  @Override
  public @NotNull Class<RespawnPacket> packetType() {
    return RespawnPacket.class;
  }

  @Override
  public @NotNull RespawnPacket decode(@NotNull ByteBuffer input) {
    return new RespawnPacket(PlayLoginCodec.readDimension(input), input.get());
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull RespawnPacket packet) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PlayLoginCodec.writeDimension(output, packet.dimension());
    output.write(packet.dataKept());
    return MinecraftWire.buffer(output);
  }
}
