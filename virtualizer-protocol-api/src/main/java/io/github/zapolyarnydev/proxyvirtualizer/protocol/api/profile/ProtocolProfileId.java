package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ProtocolProfileId(@NotNull String value) {

  public ProtocolProfileId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank())
      throw new IllegalArgumentException("Protocol profile id must not be blank");
  }

  @NotNull
  public static ProtocolProfileId of(String value) {
    return new ProtocolProfileId(value);
  }
}
