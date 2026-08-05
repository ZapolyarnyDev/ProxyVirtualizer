package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class PacketIdTest {

  @Test
  void acceptsNonNegativePacketId() {
    assertThat(new PacketId(0).value()).isZero();
    assertThat(new PacketId(42).value()).isEqualTo(42);
  }

  @Test
  void rejectsNegativePacketId() {
    assertThatIllegalArgumentException().isThrownBy(() -> new PacketId(-1));
  }
}
