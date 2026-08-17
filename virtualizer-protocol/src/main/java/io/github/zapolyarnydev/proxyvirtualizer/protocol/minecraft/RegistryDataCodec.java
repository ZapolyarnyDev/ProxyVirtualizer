package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public final class RegistryDataCodec implements PacketCodec<RegistryDataPacket> {
  @Override
  public @NotNull Class<RegistryDataPacket> packetType() {
    return RegistryDataPacket.class;
  }

  @Override
  public @NotNull RegistryDataPacket decode(@NotNull ByteBuffer input) {
    byte[] payload = new byte[input.remaining()];
    input.get(payload);
    return new RegistryDataPacket(payload);
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull RegistryDataPacket packet) {
    return ByteBuffer.wrap(packet.payload());
  }
}
