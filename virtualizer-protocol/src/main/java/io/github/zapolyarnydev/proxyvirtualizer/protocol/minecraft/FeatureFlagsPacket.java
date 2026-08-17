package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import java.util.List;
import java.util.Objects;

public record FeatureFlagsPacket(List<String> flags) implements ClientboundPacket {
  public FeatureFlagsPacket {
    flags = List.copyOf(Objects.requireNonNull(flags, "flags"));
  }
}
