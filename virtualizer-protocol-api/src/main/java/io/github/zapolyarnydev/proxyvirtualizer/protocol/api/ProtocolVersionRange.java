package io.github.zapolyarnydev.proxyvirtualizer.protocol.api;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ProtocolVersionRange(
    @NotNull ProtocolVersion minimum, @NotNull ProtocolVersion maximum) {

  public ProtocolVersionRange {
    Objects.requireNonNull(minimum, "minimum");
    Objects.requireNonNull(maximum, "maximum");
    if (minimum.compareTo(maximum) > 0)
      throw new IllegalArgumentException("Protocol version range minimum cannot exceed maximum");
  }

  @NotNull
  public static ProtocolVersionRange exact(@NotNull ProtocolVersion version) {
    Objects.requireNonNull(version, "version");
    return new ProtocolVersionRange(version, version);
  }

  public boolean contains(@NotNull ProtocolVersion version) {
    Objects.requireNonNull(version, "version");
    return version.compareTo(minimum) >= 0 && version.compareTo(maximum) <= 0;
  }

  public boolean overlaps(@NotNull ProtocolVersionRange other) {
    Objects.requireNonNull(other, "other");
    return minimum.compareTo(other.maximum) <= 0 && other.minimum.compareTo(maximum) <= 0;
  }
}
