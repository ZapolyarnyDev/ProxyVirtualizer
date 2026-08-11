package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolContext;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.OutboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.OutboundPacketEncoder;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

final class VelocityProtocolContext implements ProtocolContext {

  private final ProtocolVersion version;
  private final ProtocolProfile profile;
  private final ProtocolPhase phase;
  private final SessionTransport transport;
  private final OutboundPacketEncoder encoder;
  private final Consumer<Throwable> asynchronousFailureHandler;

  VelocityProtocolContext(
      ProtocolVersion version,
      ProtocolProfile profile,
      ProtocolPhase phase,
      SessionTransport transport,
      OutboundPacketEncoder encoder,
      Consumer<Throwable> asynchronousFailureHandler) {
    this.version = Objects.requireNonNull(version, "version");
    this.profile = Objects.requireNonNull(profile, "profile");
    this.phase = Objects.requireNonNull(phase, "phase");
    this.transport = Objects.requireNonNull(transport, "transport");
    this.encoder = Objects.requireNonNull(encoder, "encoder");
    this.asynchronousFailureHandler =
        Objects.requireNonNull(asynchronousFailureHandler, "asynchronousFailureHandler");
  }

  @Override
  public @NotNull ProtocolVersion version() {
    return version;
  }

  @Override
  public @NotNull ProtocolProfile profile() {
    return profile;
  }

  @Override
  public @NotNull ProtocolPhase phase() {
    return phase;
  }

  @Override
  public void send(@NotNull ClientboundPacket packet) {
    Objects.requireNonNull(packet, "packet");
    OutboundFrame frame = encoder.encode(version, phase, packet);
    CompletionStage<Void> completion =
        Objects.requireNonNull(transport.send(frame), "transport.send result");
    completion.whenComplete(
        (ignored, cause) -> {
          if (cause != null) asynchronousFailureHandler.accept(unwrap(cause));
        });
  }

  private static Throwable unwrap(Throwable cause) {
    if (cause instanceof CompletionException && cause.getCause() != null) return cause.getCause();
    return cause;
  }
}
