package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import java.util.Objects;

public record RespawnPacket(PlayLoginPacket.DimensionState dimension, byte dataKept)
    implements ClientboundPacket {
  public RespawnPacket {
    dimension = Objects.requireNonNull(dimension, "dimension");
  }
}
