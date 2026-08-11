package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.OutboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.PacketEncodingException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.UnknownPacketCodecException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.RegisteredPacketCodec;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class OutboundPacketEncoder {

  private final ProtocolRegistry registry;

  public OutboundPacketEncoder(@NotNull ProtocolRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  public @NotNull OutboundFrame encode(
      @NotNull ProtocolVersion version,
      @NotNull ProtocolPhase phase,
      @NotNull ClientboundPacket packet) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(packet, "packet");

    RegisteredPacketCodec<ClientboundPacket> registration =
        registry
            .codecs()
            .findClientbound(version, phase, clientboundPacketType(packet))
            .orElseThrow(
                () ->
                    new UnknownPacketCodecException(
                        "No clientbound packet codec for protocol "
                            + version.number()
                            + ", phase "
                            + phase
                            + ", packet "
                            + packet.getClass().getName()));
    ByteBuffer payload;
    try {
      payload =
          Objects.requireNonNull(
              registration.codec().encode(packet), "packet codec returned payload");
    } catch (RuntimeException cause) {
      throw new PacketEncodingException(
          "Could not encode clientbound packet "
              + packet.getClass().getName()
              + " for protocol "
              + version.number()
              + ", phase "
              + phase,
          cause);
    }
    return new OutboundFrame(registration.packetId(), payload);
  }

  @SuppressWarnings("unchecked")
  private static Class<ClientboundPacket> clientboundPacketType(ClientboundPacket packet) {
    return (Class<ClientboundPacket>) packet.getClass();
  }
}
