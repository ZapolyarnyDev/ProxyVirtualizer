package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.OutboundPacketEncoder;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.ProtocolSessionProcessor;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class VelocitySessionProtocolFactory {

  private final ProtocolSessionProcessor processor;
  private final OutboundPacketEncoder encoder;
  private final LongSupplier keepAliveIds;
  private final ScheduledExecutorService scheduler;
  private final Duration heartbeatTimeout;

  public VelocitySessionProtocolFactory(
      ProtocolRegistry registry,
      LongSupplier keepAliveIds,
      ScheduledExecutorService scheduler,
      Duration heartbeatTimeout) {
    Objects.requireNonNull(registry, "registry");
    processor = new ProtocolSessionProcessor(registry);
    encoder = new OutboundPacketEncoder(registry);
    this.keepAliveIds = Objects.requireNonNull(keepAliveIds, "keepAliveIds");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.heartbeatTimeout = Objects.requireNonNull(heartbeatTimeout, "heartbeatTimeout");
  }

  public VelocitySessionProtocol create(
      ProtocolVersion version,
      ProtocolProfile profile,
      ProtocolPhase phase,
      SessionTransport transport,
      Consumer<Throwable> asynchronousFailureHandler) {
    VelocityProtocolContext context =
        new VelocityProtocolContext(
            version, profile, phase, transport, encoder, asynchronousFailureHandler);
    return new VelocitySessionProtocol(
        processor,
        context,
        new SessionHeartbeat(keepAliveIds),
        scheduler,
        heartbeatTimeout,
        asynchronousFailureHandler);
  }
}
