package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import java.util.Arrays;
import java.util.Objects;

public final class RegistryDataPacket implements ClientboundPacket {
  private final byte[] payload;

  public RegistryDataPacket(byte[] payload) {
    this.payload = Arrays.copyOf(Objects.requireNonNull(payload, "payload"), payload.length);
  }

  public byte[] payload() {
    return Arrays.copyOf(payload, payload.length);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof RegistryDataPacket packet && Arrays.equals(payload, packet.payload);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(payload);
  }
}
