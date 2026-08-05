package io.github.zapolyarnydev.proxyvirtualizer.protocol.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class ProtocolVersionTest {

  @Test
  void acceptsPositiveProtocolNumber() {
    ProtocolVersion version = new ProtocolVersion(769);

    assertThat(version.number()).isEqualTo(769);
  }

  @Test
  void rejectsNonPositiveProtocolNumber() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ProtocolVersion(0));
    assertThatIllegalArgumentException().isThrownBy(() -> new ProtocolVersion(-1));
  }
}
