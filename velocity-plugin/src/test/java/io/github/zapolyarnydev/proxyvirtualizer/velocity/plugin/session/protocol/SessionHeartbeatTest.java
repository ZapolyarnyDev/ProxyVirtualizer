package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.KeepAliveAcknowledged;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

final class SessionHeartbeatTest {

  private static final long KEEP_ALIVE_ID = 42L;

  @Test
  void closeCancelsArmedTimeoutAndPreventsExpiration() {
    SessionHeartbeat heartbeat = startedHeartbeat();
    TestScheduledFuture timeout = new TestScheduledFuture();
    heartbeat.armTimeout(timeout);

    heartbeat.close();

    assertThat(timeout.isCancelled()).isTrue();
    assertThat(heartbeat.expire()).isFalse();
  }

  @Test
  void timeoutArmedAfterCloseIsCancelledImmediately() {
    SessionHeartbeat heartbeat = startedHeartbeat();
    heartbeat.close();
    TestScheduledFuture timeout = new TestScheduledFuture();

    heartbeat.armTimeout(timeout);

    assertThat(timeout.isCancelled()).isTrue();
    assertThat(heartbeat.expire()).isFalse();
  }

  @Test
  void acknowledgementCancelsTimeoutAndPreventsExpiration() {
    SessionHeartbeat heartbeat = startedHeartbeat();
    TestScheduledFuture timeout = new TestScheduledFuture();
    heartbeat.armTimeout(timeout);

    heartbeat.acknowledge(new KeepAliveAcknowledged(KEEP_ALIVE_ID));

    assertThat(timeout.isCancelled()).isTrue();
    assertThat(heartbeat.isAcknowledged()).isTrue();
    assertThat(heartbeat.expire()).isFalse();
  }

  @Test
  void expirationWinsExactlyOnceAndRejectsLateAcknowledgement() {
    SessionHeartbeat heartbeat = startedHeartbeat();

    assertThat(heartbeat.expire()).isTrue();
    assertThat(heartbeat.expire()).isFalse();
    assertThatThrownBy(() -> heartbeat.acknowledge(new KeepAliveAcknowledged(KEEP_ALIVE_ID)))
        .isInstanceOf(HeartbeatProtocolException.class)
        .hasMessageContaining("TIMED_OUT");
  }

  @Test
  void rejectsSecondTimeoutWithoutReplacingActiveTimeout() {
    SessionHeartbeat heartbeat = startedHeartbeat();
    TestScheduledFuture first = new TestScheduledFuture();
    TestScheduledFuture second = new TestScheduledFuture();
    heartbeat.armTimeout(first);

    assertThatThrownBy(() -> heartbeat.armTimeout(second))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already been armed");
    assertThat(first.isCancelled()).isFalse();
    assertThat(second.isCancelled()).isTrue();
  }

  private static SessionHeartbeat startedHeartbeat() {
    SessionHeartbeat heartbeat = new SessionHeartbeat(() -> KEEP_ALIVE_ID);
    heartbeat.begin();
    return heartbeat;
  }

  private static final class TestScheduledFuture implements ScheduledFuture<Object> {

    private boolean cancelled;

    @Override
    public long getDelay(TimeUnit unit) {
      return 0;
    }

    @Override
    public int compareTo(Delayed other) {
      return 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (cancelled) return false;
      cancelled = true;
      return true;
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean isDone() {
      return cancelled;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      throw new UnsupportedOperationException();
    }
  }
}
