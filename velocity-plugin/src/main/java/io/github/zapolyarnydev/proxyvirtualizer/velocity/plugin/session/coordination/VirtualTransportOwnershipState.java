package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

enum VirtualTransportOwnershipState {
  BACKEND_BOUND,
  SWITCHING,
  VIRTUAL_BOUND,
  CLOSING;

  VirtualTransportOwnershipState beginSwitching() {
    if (this != BACKEND_BOUND)
      throw new IllegalStateException("Virtual ownership is already claimed");
    return SWITCHING;
  }

  VirtualTransportOwnershipState completeSwitching() {
    if (this != SWITCHING) throw new IllegalStateException("Virtual ownership is not switching");
    return VIRTUAL_BOUND;
  }

  VirtualTransportOwnershipState beginClosing() {
    if (this == CLOSING) throw new IllegalStateException("Virtual ownership is already closing");
    return CLOSING;
  }
}
