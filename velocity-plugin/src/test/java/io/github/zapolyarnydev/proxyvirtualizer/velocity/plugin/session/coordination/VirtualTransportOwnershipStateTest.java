package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class VirtualTransportOwnershipStateTest {

  @Test
  void transitionsFromBackendToVirtualOwnership() {
    VirtualTransportOwnershipState switching =
        VirtualTransportOwnershipState.BACKEND_BOUND.beginSwitching();

    assertThat(switching.completeSwitching())
        .isEqualTo(VirtualTransportOwnershipState.VIRTUAL_BOUND);
  }

  @Test
  void transitionsEveryActiveStateToClosing() {
    assertThat(VirtualTransportOwnershipState.BACKEND_BOUND.beginClosing())
        .isEqualTo(VirtualTransportOwnershipState.CLOSING);
    assertThat(VirtualTransportOwnershipState.SWITCHING.beginClosing())
        .isEqualTo(VirtualTransportOwnershipState.CLOSING);
    assertThat(VirtualTransportOwnershipState.VIRTUAL_BOUND.beginClosing())
        .isEqualTo(VirtualTransportOwnershipState.CLOSING);
  }

  @Test
  void rejectsInvalidOwnershipTransitions() {
    assertThatThrownBy(VirtualTransportOwnershipState.BACKEND_BOUND::completeSwitching)
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(VirtualTransportOwnershipState.VIRTUAL_BOUND::beginSwitching)
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(VirtualTransportOwnershipState.CLOSING::beginClosing)
        .isInstanceOf(IllegalStateException.class);
  }
}
