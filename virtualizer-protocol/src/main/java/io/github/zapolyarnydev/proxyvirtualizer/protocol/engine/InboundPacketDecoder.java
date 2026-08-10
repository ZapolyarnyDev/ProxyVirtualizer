package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.codec.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.PacketDirection;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ProtocolPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.frame.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.MalformedPacketException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.UnknownPacketCodecException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class InboundPacketDecoder {

  private final ProtocolRegistry registry;

  public InboundPacketDecoder(@NotNull ProtocolRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  public @NotNull ServerboundPacket decode(
      @NotNull ProtocolVersion version, @NotNull ProtocolPhase phase, @NotNull InboundFrame frame) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(frame, "frame");

    PacketCodec<?> codec =
        registry
            .codecs()
            .find(version, phase, PacketDirection.SERVERBOUND, frame.packetId())
            .orElseThrow(
                () ->
                    new UnknownPacketCodecException(
                        "No serverbound packet codec for protocol "
                            + version.number()
                            + ", phase "
                            + phase
                            + ", packet "
                            + frame.packetId().value()));
    ByteBuffer payload = frame.payload();
    ProtocolPacket packet;
    try {
      packet = codec.decode(payload);
    } catch (RuntimeException exception) {
      throw malformed(version, phase, frame, "could not decode payload", exception);
    }

    if (payload.hasRemaining()) {
      throw malformed(
          version,
          phase,
          frame,
          "codec left " + payload.remaining() + " unread payload byte(s)",
          null);
    }
    if (!(packet instanceof ServerboundPacket serverboundPacket)) {
      throw malformed(version, phase, frame, "codec returned a non-serverbound packet", null);
    }
    return serverboundPacket;
  }

  private static MalformedPacketException malformed(
      ProtocolVersion version,
      ProtocolPhase phase,
      InboundFrame frame,
      String detail,
      RuntimeException cause) {
    String message =
        "Malformed serverbound packet for protocol "
            + version.number()
            + ", phase "
            + phase
            + ", packet "
            + frame.packetId().value()
            + ": "
            + detail;
    return cause == null
        ? new MalformedPacketException(message)
        : new MalformedPacketException(message, cause);
  }
}
