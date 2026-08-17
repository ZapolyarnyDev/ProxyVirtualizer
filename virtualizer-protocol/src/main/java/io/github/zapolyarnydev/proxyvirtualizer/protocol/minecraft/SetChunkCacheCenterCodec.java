package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public final class SetChunkCacheCenterCodec implements PacketCodec<SetChunkCacheCenterPacket> {
  @Override
  public @NotNull Class<SetChunkCacheCenterPacket> packetType() {
    return SetChunkCacheCenterPacket.class;
  }

  @Override
  public @NotNull SetChunkCacheCenterPacket decode(@NotNull ByteBuffer input) {
    return new SetChunkCacheCenterPacket(
        MinecraftWire.readVarInt(input), MinecraftWire.readVarInt(input));
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull SetChunkCacheCenterPacket packet) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    MinecraftWire.writeVarInt(output, packet.chunkX());
    MinecraftWire.writeVarInt(output, packet.chunkZ());
    return MinecraftWire.buffer(output);
  }
}
