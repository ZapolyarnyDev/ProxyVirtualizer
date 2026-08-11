package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.KeepAliveAcknowledged;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft.ClientboundKeepAlivePacket;
import java.util.Objects;
import java.util.function.LongSupplier;

final class SessionHeartbeat {

  private final LongSupplier idSource;
  private State state = State.NEW;
  private long expectedId;

  SessionHeartbeat(LongSupplier idSource) {
    this.idSource = Objects.requireNonNull(idSource, "idSource");
  }

  synchronized ClientboundKeepAlivePacket begin() {
    if (state != State.NEW) {
      throw new IllegalStateException("Session heartbeat has already been started");
    }

    expectedId = idSource.getAsLong();
    state = State.AWAITING_ACKNOWLEDGEMENT;
    return new ClientboundKeepAlivePacket(expectedId);
  }

  synchronized void acknowledge(KeepAliveAcknowledged acknowledgement) {
    Objects.requireNonNull(acknowledgement, "acknowledgement");
    if (state != State.AWAITING_ACKNOWLEDGEMENT) {
      throw new HeartbeatProtocolException(
          "Unexpected KeepAlive acknowledgement while heartbeat state is " + state);
    }
    if (acknowledgement.id() != expectedId) {
      throw new HeartbeatProtocolException(
          "KeepAlive acknowledgement id "
              + acknowledgement.id()
              + " does not match expected id "
              + expectedId);
    }
    state = State.ACKNOWLEDGED;
  }

  synchronized boolean isAcknowledged() {
    return state == State.ACKNOWLEDGED;
  }

  private enum State {
    NEW,
    AWAITING_ACKNOWLEDGEMENT,
    ACKNOWLEDGED
  }
}
