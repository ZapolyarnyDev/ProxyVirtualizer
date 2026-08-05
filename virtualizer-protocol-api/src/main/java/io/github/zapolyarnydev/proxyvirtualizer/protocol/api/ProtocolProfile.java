package io.github.zapolyarnydev.proxyvirtualizer.protocol.api;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public interface ProtocolProfile {

  @NotNull
  ProtocolVersion version();

  @NotNull
  Set<ProtocolCapability> capabilities();

  default boolean supports(@NotNull ProtocolCapability capability) {
    Objects.requireNonNull(capability, "capability");
    return capabilities().contains(capability);
  }

  default boolean supportsAll(Collection<? extends ProtocolCapability> capabilities) {
    Objects.requireNonNull(capabilities, "capabilities");
    return capabilities().stream().allMatch(this::supports);
  }

  default boolean supportsAny(Collection<? extends ProtocolCapability> capabilities) {
    Objects.requireNonNull(capabilities, "capabilities");
    return capabilities().stream().anyMatch(this::supports);
  }
}
