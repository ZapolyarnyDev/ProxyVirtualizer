package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ProtocolVersion(int number) implements Comparable<ProtocolVersion> {

  public ProtocolVersion {
    if (number <= 0) throw new IllegalArgumentException("Protocol version must be positive");
  }

  public static ProtocolVersion of(int value) {
    return new ProtocolVersion(value);
  }

  public boolean isNewerThan(ProtocolVersion other) {
    Objects.requireNonNull(other, "other");
    return compareTo(other) > 0;
  }

  public boolean isOlderThan(ProtocolVersion other) {
    Objects.requireNonNull(other, "other");
    return compareTo(other) < 0;
  }

  @Override
  public int compareTo(@NotNull ProtocolVersion other) {
    Objects.requireNonNull(other, "other");
    return Integer.compare(number, other.number);
  }
}
