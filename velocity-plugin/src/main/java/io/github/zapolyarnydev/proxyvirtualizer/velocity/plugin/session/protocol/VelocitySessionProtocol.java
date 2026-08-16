package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.KeepAliveAcknowledged;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.ProtocolSessionProcessor;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.SemanticActionRouter;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

public final class VelocitySessionProtocol {

  private final ProtocolSessionProcessor processor;
  private final VelocityProtocolContext context;
  private final SessionHeartbeat heartbeat;
  private final SemanticActionRouter actions;
  private final ScheduledExecutorService scheduler;
  private final Duration heartbeatTimeout;
  private final java.util.function.Consumer<Throwable> asynchronousFailureHandler;

  VelocitySessionProtocol(
      ProtocolSessionProcessor processor,
      VelocityProtocolContext context,
      SessionHeartbeat heartbeat,
      ScheduledExecutorService scheduler,
      Duration heartbeatTimeout,
      java.util.function.Consumer<Throwable> asynchronousFailureHandler) {
    this.processor = Objects.requireNonNull(processor, "processor");
    this.context = Objects.requireNonNull(context, "context");
    this.heartbeat = Objects.requireNonNull(heartbeat, "heartbeat");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.heartbeatTimeout = Objects.requireNonNull(heartbeatTimeout, "heartbeatTimeout");
    this.asynchronousFailureHandler =
        Objects.requireNonNull(asynchronousFailureHandler, "asynchronousFailureHandler");
    actions =
        SemanticActionRouter.builder()
            .route(KeepAliveAcknowledged.class, heartbeat::acknowledge)
            .build();
  }

  public void onOpened() {
    context.send(heartbeat.begin());
    heartbeat.armTimeout(
        scheduler.schedule(
            () -> {
              if (heartbeat.expire())
                asynchronousFailureHandler.accept(
                    new HeartbeatTimeoutException(
                        "KeepAlive acknowledgement was not received within " + heartbeatTimeout));
            },
            heartbeatTimeout.toMillis(),
            java.util.concurrent.TimeUnit.MILLISECONDS));
  }

  public void onInboundFrame(InboundFrame frame) {
    processor.process(context, Objects.requireNonNull(frame, "frame"), actions);
  }

  boolean isHeartbeatAcknowledged() {
    return heartbeat.isAcknowledged();
  }

  public void close() {
    heartbeat.close();
  }
}
