package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile;

import java.util.Objects;

public record ProtocolCapability(String key) {

  public ProtocolCapability {
    Objects.requireNonNull(key, "key");

    if (key.isBlank())
      throw new IllegalArgumentException("Protocol capability key must not be blank");
  }

  public static ProtocolCapability of(String key) {
    return new ProtocolCapability(key);
  }
}
