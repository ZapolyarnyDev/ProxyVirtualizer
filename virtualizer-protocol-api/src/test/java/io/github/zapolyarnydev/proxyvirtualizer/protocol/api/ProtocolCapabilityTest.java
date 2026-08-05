package io.github.zapolyarnydev.proxyvirtualizer.protocol.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

final class ProtocolCapabilityTest {

  @Test
  void acceptsNonBlankKey() {
    ProtocolCapability capability = new ProtocolCapability("virtual-session");

    assertThat(capability.key()).isEqualTo("virtual-session");
  }

  @Test
  void rejectsBlankOrNullKey() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ProtocolCapability(" "));
    assertThatNullPointerException().isThrownBy(() -> new ProtocolCapability(null));
  }
}
