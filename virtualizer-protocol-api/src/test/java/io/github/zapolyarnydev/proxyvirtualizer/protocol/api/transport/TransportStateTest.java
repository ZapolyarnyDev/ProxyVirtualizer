package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class TransportStateTest {

  @Test
  void identifiesOpenState() {
    assertThat(TransportState.OPEN.isOpen()).isTrue();
    assertThat(TransportState.NEW.isOpen()).isFalse();
    assertThat(TransportState.CLOSING.isOpen()).isFalse();
  }

  @Test
  void identifiesTerminalStates() {
    assertThat(TransportState.CLOSED.isTerminal()).isTrue();
    assertThat(TransportState.FAILED.isTerminal()).isTrue();
    assertThat(TransportState.CLOSING.isTerminal()).isFalse();
  }
}
