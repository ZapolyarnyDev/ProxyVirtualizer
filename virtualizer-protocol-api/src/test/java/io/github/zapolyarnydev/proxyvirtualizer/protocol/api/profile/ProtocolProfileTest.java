package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile;

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
          public @NotNull ProtocolProfileId id() {
            return new ProtocolProfileId("minecraft-1.21");
          }

          @Override
          public @NotNull ProtocolVersionRange versions() {
            return ProtocolVersionRange.exact(new ProtocolVersion(769));
          }

          @Override
          public @NotNull Set<ProtocolPhase> phases() {
            return Set.of(ProtocolPhase.LOGIN, ProtocolPhase.PLAY);
          }

          @Override
          public @NotNull Set<ProtocolCapability> capabilities() {
            return Set.of(VIRTUAL_SESSION);
          }
        };

    assertThat(profile.supports(VIRTUAL_SESSION)).isTrue();
    assertThat(profile.supports(KEEP_ALIVE)).isFalse();
    assertThat(profile.supports(new ProtocolVersion(769))).isTrue();
    assertThat(profile.supports(new ProtocolVersion(770))).isFalse();
    assertThat(profile.supports(ProtocolPhase.LOGIN)).isTrue();
    assertThat(profile.supports(ProtocolPhase.CONFIGURATION)).isFalse();
  }
}
