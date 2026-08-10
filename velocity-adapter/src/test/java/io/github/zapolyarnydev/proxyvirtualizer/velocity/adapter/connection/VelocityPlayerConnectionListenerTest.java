package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

final class VelocityPlayerConnectionListenerTest {

  @Test
  void reportsSynchronousAndAsynchronousLifecycleFailures() {
    List<Throwable> failures = new ArrayList<>();
    VelocityPlayerConnectionListener listener =
        new VelocityPlayerConnectionListener(
            new VelocityPlayerConnectionLifecycle(
                new NoopLifecycle(), new VelocityConnectionRegistry()),
            failures::add);
    IllegalStateException synchronous = new IllegalStateException("sync");
    IllegalStateException asynchronous = new IllegalStateException("async");

    listener.observe(
        () -> {
          throw synchronous;
        });
    listener.observe(() -> CompletableFuture.failedFuture(asynchronous));

    assertThat(failures).containsExactly(synchronous, asynchronous);
  }

  private static final class NoopLifecycle implements PlayerConnectionLifecycle {

    @Override
    public CompletionStage<Void> playerConnected(PlayerId playerId, ConnectionId connectionId) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> playerDisconnected(ConnectionId connectionId) {
      return CompletableFuture.completedFuture(null);
    }
  }
}
