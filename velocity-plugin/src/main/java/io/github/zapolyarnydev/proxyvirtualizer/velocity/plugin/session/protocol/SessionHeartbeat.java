package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.KeepAliveAcknowledged;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft.ClientboundKeepAlivePacket;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.function.LongSupplier;

final class SessionHeartbeat {

  private final LongSupplier idSource;
  private State state = State.NEW;
  private long expectedId;
  private ScheduledFuture<?> timeout;

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
    cancelTimeout();
  }

  synchronized void armTimeout(ScheduledFuture<?> timeout) {
    Objects.requireNonNull(timeout, "timeout");
    if (state != State.AWAITING_ACKNOWLEDGEMENT) {
      timeout.cancel(false);
      return;
    }
    if (this.timeout != null) {
      timeout.cancel(false);
      throw new IllegalStateException("Session heartbeat timeout has already been armed");
    }
    this.timeout = timeout;
  }

  synchronized boolean expire() {
    if (state != State.AWAITING_ACKNOWLEDGEMENT) return false;

    state = State.TIMED_OUT;
    timeout = null;
    return true;
  }

  synchronized void close() {
    state = State.CLOSED;
    cancelTimeout();
  }

  synchronized boolean isAcknowledged() {
    return state == State.ACKNOWLEDGED;
  }

  private void cancelTimeout() {
    ScheduledFuture<?> activeTimeout = timeout;
    timeout = null;
    if (activeTimeout != null) activeTimeout.cancel(false);
  }

  private enum State {
    NEW,
    AWAITING_ACKNOWLEDGEMENT,
    ACKNOWLEDGED,
    TIMED_OUT,
    CLOSED
  }
}
