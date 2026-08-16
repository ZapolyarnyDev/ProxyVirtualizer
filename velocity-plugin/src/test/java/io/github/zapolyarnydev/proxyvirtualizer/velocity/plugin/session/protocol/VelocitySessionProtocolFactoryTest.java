package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class VelocitySessionProtocolFactoryTest {

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  @AfterEach
  void closeScheduler() {
    scheduler.shutdownNow();
  }

  @Test
  void rejectsNonPositiveHeartbeatTimeout() {
    ProtocolRegistry registry = new ProtocolRegistry();

    assertThatThrownBy(
            () -> new VelocitySessionProtocolFactory(registry, () -> 1L, scheduler, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be positive");
    assertThatThrownBy(
            () ->
                new VelocitySessionProtocolFactory(
                    registry, () -> 1L, scheduler, Duration.ofNanos(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be positive");
  }

  @Test
  void acceptsSubMillisecondHeartbeatTimeout() {
    assertThatCode(
            () ->
                new VelocitySessionProtocolFactory(
                    new ProtocolRegistry(), () -> 1L, scheduler, Duration.ofNanos(1)))
        .doesNotThrowAnyException();
  }
}
