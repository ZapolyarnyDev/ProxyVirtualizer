package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.SessionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol.VelocitySessionProtocol;
import java.util.Objects;

final class VelocitySessionTransportBinding {

  private final SessionSnapshot session;
  private final SessionTransport transport;
  private final VelocitySessionProtocol protocol;
  private VirtualTransportOwnershipState ownership = VirtualTransportOwnershipState.BACKEND_BOUND;

  VelocitySessionTransportBinding(
      SessionSnapshot session, SessionTransport transport, VelocitySessionProtocol protocol) {
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(transport, "transport");
    Objects.requireNonNull(protocol, "protocol");
    this.session = session;
    this.transport = transport;
    this.protocol = protocol;
  }

  SessionSnapshot session() {
    return session;
  }

  SessionTransport transport() {
    return transport;
  }

  VelocitySessionProtocol protocol() {
    return protocol;
  }

  synchronized void beginSwitching() {
    ownership = ownership.beginSwitching();
  }

  synchronized boolean completeSwitching() {
    if (ownership == VirtualTransportOwnershipState.CLOSING) return false;
    ownership = ownership.completeSwitching();
    return true;
  }

  synchronized boolean beginClosing() {
    if (ownership == VirtualTransportOwnershipState.CLOSING) return false;
    ownership = ownership.beginClosing();
    return true;
  }

  synchronized boolean isSwitching() {
    return ownership == VirtualTransportOwnershipState.SWITCHING;
  }

  synchronized VirtualTransportOwnershipState ownership() {
    return ownership;
  }
}
