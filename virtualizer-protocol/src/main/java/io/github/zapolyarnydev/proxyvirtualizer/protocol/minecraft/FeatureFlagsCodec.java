package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public final class FeatureFlagsCodec implements PacketCodec<FeatureFlagsPacket> {
  @Override
  public @NotNull Class<FeatureFlagsPacket> packetType() {
    return FeatureFlagsPacket.class;
  }

  @Override
  public @NotNull FeatureFlagsPacket decode(@NotNull ByteBuffer input) {
    return new FeatureFlagsPacket(MinecraftWire.readStringList(input));
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull FeatureFlagsPacket packet) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    MinecraftWire.writeStringList(output, packet.flags());
    return MinecraftWire.buffer(output);
  }
}
