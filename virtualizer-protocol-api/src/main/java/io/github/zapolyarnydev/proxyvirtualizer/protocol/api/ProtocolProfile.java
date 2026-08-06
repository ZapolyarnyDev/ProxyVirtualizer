package io.github.zapolyarnydev.proxyvirtualizer.protocol.api;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public interface ProtocolProfile {

  @NotNull
  ProtocolProfileId id();

  @NotNull
  ProtocolVersionRange versions();

  @NotNull
  Set<ProtocolPhase> phases();

  @NotNull
  Set<ProtocolCapability> capabilities();

  default boolean supports(@NotNull ProtocolVersion version) {
    Objects.requireNonNull(version, "version");
    return versions().contains(version);
  }

  default boolean supports(@NotNull ProtocolPhase phase) {
    Objects.requireNonNull(phase, "phase");
    return phases().contains(phase);
  }

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
