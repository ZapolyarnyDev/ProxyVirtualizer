package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.KeepAliveAcknowledged;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.ProtocolSessionProcessor;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.SemanticActionRouter;
import java.util.Objects;

public final class VelocitySessionProtocol {

  private final ProtocolSessionProcessor processor;
  private final VelocityProtocolContext context;
  private final SessionHeartbeat heartbeat;
  private final SemanticActionRouter actions;

  VelocitySessionProtocol(
      ProtocolSessionProcessor processor,
      VelocityProtocolContext context,
      SessionHeartbeat heartbeat) {
    this.processor = Objects.requireNonNull(processor, "processor");
    this.context = Objects.requireNonNull(context, "context");
    this.heartbeat = Objects.requireNonNull(heartbeat, "heartbeat");
    actions =
        SemanticActionRouter.builder()
            .route(KeepAliveAcknowledged.class, heartbeat::acknowledge)
            .build();
  }

  public void onOpened() {
    context.send(heartbeat.begin());
  }

  public void onInboundFrame(InboundFrame frame) {
    processor.process(context, Objects.requireNonNull(frame, "frame"), actions);
  }

  boolean isHeartbeatAcknowledged() {
    return heartbeat.isAcknowledged();
  }
}
