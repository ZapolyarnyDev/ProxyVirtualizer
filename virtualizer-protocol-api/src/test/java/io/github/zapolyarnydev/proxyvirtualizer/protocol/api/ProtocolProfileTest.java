package io.github.zapolyarnydev.proxyvirtualizer.protocol.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

final class ProtocolProfileTest {

  private static final ProtocolCapability VIRTUAL_SESSION =
      new ProtocolCapability("virtual-session");
  private static final ProtocolCapability KEEP_ALIVE = new ProtocolCapability("keep-alive");

  @Test
  void reportsSupportedCapabilities() {
    ProtocolProfile profile =
        new ProtocolProfile() {
          @Override
          public @NotNull ProtocolVersion version() {
            return new ProtocolVersion(769);
          }

          @Override
          public @NotNull Set<ProtocolCapability> capabilities() {
            return Set.of(VIRTUAL_SESSION);
          }
        };

    assertThat(profile.supports(VIRTUAL_SESSION)).isTrue();
    assertThat(profile.supports(KEEP_ALIVE)).isFalse();
  }
}
