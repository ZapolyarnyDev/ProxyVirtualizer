package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.SessionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import java.util.Objects;

record VelocitySessionTransportBinding(SessionSnapshot session, SessionTransport transport) {

  VelocitySessionTransportBinding {
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(transport, "transport");
  }
}
