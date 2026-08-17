package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public final class SynchronizePlayerPositionCodec
    implements PacketCodec<SynchronizePlayerPositionPacket> {
  @Override
  public @NotNull Class<SynchronizePlayerPositionPacket> packetType() {
    return SynchronizePlayerPositionPacket.class;
  }

  @Override
  public @NotNull SynchronizePlayerPositionPacket decode(@NotNull ByteBuffer input) {
    return new SynchronizePlayerPositionPacket(
        MinecraftWire.readVarInt(input),
        input.getDouble(),
        input.getDouble(),
        input.getDouble(),
        input.getDouble(),
        input.getDouble(),
        input.getDouble(),
        input.getFloat(),
        input.getFloat(),
        input.getInt());
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull SynchronizePlayerPositionPacket packet) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    MinecraftWire.writeVarInt(output, packet.teleportId());
    ByteBuffer values =
        ByteBuffer.allocate(64)
            .putDouble(packet.x())
            .putDouble(packet.y())
            .putDouble(packet.z())
            .putDouble(packet.velocityX())
            .putDouble(packet.velocityY())
            .putDouble(packet.velocityZ())
            .putFloat(packet.yaw())
            .putFloat(packet.pitch())
            .putInt(packet.flags());
    output.writeBytes(values.array());
    return MinecraftWire.buffer(output);
  }
}
