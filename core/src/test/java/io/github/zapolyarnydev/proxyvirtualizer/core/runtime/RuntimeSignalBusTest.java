package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RoomRegisteredSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RuntimeSignal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimeSignalBusTest {

  private static final RuntimeSignal SIGNAL =
      new RoomRegisteredSignal(new RoomSnapshot(new RoomId(1), 0, false));

  @Test
  void notifiesListenersInSubscriptionOrder() {
    RuntimeSignalBus bus = new RuntimeSignalBus();
    List<String> notifications = new ArrayList<>();
    bus.subscribe(signal -> notifications.add("first"));
    bus.subscribe(signal -> notifications.add("second"));

    bus.dispatch(SIGNAL);

    assertThat(notifications).containsExactly("first", "second");
  }

  @Test
  void stopsNotifyingUnsubscribedListener() {
    RuntimeSignalBus bus = new RuntimeSignalBus();
    List<RuntimeSignal> notifications = new ArrayList<>();
    RuntimeSignalListener listener = notifications::add;
    bus.subscribe(listener);
    bus.dispatch(SIGNAL);

    assertThat(bus.unsubscribe(listener)).isTrue();
    bus.dispatch(SIGNAL);

    assertThat(notifications).containsExactly(SIGNAL);
  }

  @Test
  void isolatesListenerFailureAndReportsIt() {
    IllegalStateException failure = new IllegalStateException("boom");
    List<DispatchFailure> failures = new ArrayList<>();
    RuntimeSignalBus bus =
        new RuntimeSignalBus(
            (listener, signal, cause) ->
                failures.add(new DispatchFailure(listener, signal, cause)));
    RuntimeSignalListener failingListener =
        signal -> {
          throw failure;
        };
    List<RuntimeSignal> notifications = new ArrayList<>();
    bus.subscribe(failingListener);
    bus.subscribe(notifications::add);

    bus.dispatch(SIGNAL);

    assertThat(notifications).containsExactly(SIGNAL);
    assertThat(failures).containsExactly(new DispatchFailure(failingListener, SIGNAL, failure));
  }

  @Test
  void ignoresDuplicateSubscription() {
    RuntimeSignalBus bus = new RuntimeSignalBus();
    List<RuntimeSignal> notifications = new ArrayList<>();
    RuntimeSignalListener listener = notifications::add;

    assertThat(bus.subscribe(listener)).isTrue();
    assertThat(bus.subscribe(listener)).isFalse();
    bus.dispatch(SIGNAL);

    assertThat(notifications).containsExactly(SIGNAL);
  }

  private record DispatchFailure(
      RuntimeSignalListener listener, RuntimeSignal signal, Throwable cause) {}
}
