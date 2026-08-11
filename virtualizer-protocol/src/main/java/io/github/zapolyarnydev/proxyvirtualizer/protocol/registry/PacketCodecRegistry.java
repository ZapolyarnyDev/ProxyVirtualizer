package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketDirection;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception.DuplicatePacketCodecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PacketCodecRegistry {

  private final ProtocolProfileRegistry profiles;
  private final Map<CodecKey, PacketCodec<?>> codecs = new HashMap<>();
  private final Map<EncoderKey, RegisteredPacketCodec<?>> clientboundCodecs = new HashMap<>();

  PacketCodecRegistry(ProtocolProfileRegistry profiles) {
    this.profiles = Objects.requireNonNull(profiles, "profiles");
  }

  public <P extends ServerboundPacket> void registerServerbound(
      ProtocolProfileId profileId, ProtocolPhase phase, PacketId packetId, PacketCodec<P> codec) {
    register(profileId, phase, PacketDirection.SERVERBOUND, packetId, codec);
  }

  public <P extends ClientboundPacket> void registerClientbound(
      ProtocolProfileId profileId, ProtocolPhase phase, PacketId packetId, PacketCodec<P> codec) {
    register(profileId, phase, PacketDirection.CLIENTBOUND, packetId, codec);
  }

  private synchronized <P extends ProtocolPacket> void register(
      ProtocolProfileId profileId,
      ProtocolPhase phase,
      PacketDirection direction,
      PacketId packetId,
      PacketCodec<P> codec) {
    Objects.requireNonNull(profileId, "profileId");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(packetId, "packetId");
    Objects.requireNonNull(codec, "codec");
    Class<P> packetType = Objects.requireNonNull(codec.packetType(), "codec.packetType()");
    requireDirection(packetType, direction);
    profiles.require(profileId, phase);
    CodecKey key = new CodecKey(profileId, phase, direction, packetId);
    if (codecs.containsKey(key)) {
      throw new DuplicatePacketCodecException("Packet codec is already registered: " + key);
    }

    EncoderKey encoderKey = null;
    if (direction == PacketDirection.CLIENTBOUND) {
      encoderKey = new EncoderKey(profileId, phase, packetType);
      if (clientboundCodecs.containsKey(encoderKey)) {
        throw new DuplicatePacketCodecException(
            "Clientbound packet type is already registered: " + encoderKey);
      }
    }

    codecs.put(key, codec);
    if (encoderKey != null)
      clientboundCodecs.put(encoderKey, new RegisteredPacketCodec<>(packetId, codec));
  }

  public Optional<PacketCodec<?>> find(
      ProtocolVersion version, ProtocolPhase phase, PacketDirection direction, PacketId packetId) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(packetId, "packetId");
    ProtocolProfileId profileId = profiles.require(version, phase).id();
    return Optional.ofNullable(codecs.get(new CodecKey(profileId, phase, direction, packetId)));
  }

  public <P extends ClientboundPacket> Optional<RegisteredPacketCodec<P>> findClientbound(
      ProtocolVersion version, ProtocolPhase phase, Class<P> packetType) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(packetType, "packetType");
    ProtocolProfileId profileId = profiles.require(version, phase).id();
    return Optional.ofNullable(
        castRegistration(clientboundCodecs.get(new EncoderKey(profileId, phase, packetType))));
  }

  private static void requireDirection(
      Class<? extends ProtocolPacket> packetType, PacketDirection direction) {
    Class<? extends ProtocolPacket> expectedType =
        direction == PacketDirection.SERVERBOUND
            ? ServerboundPacket.class
            : ClientboundPacket.class;
    if (!expectedType.isAssignableFrom(packetType)) {
      throw new IllegalArgumentException(
          "Packet type " + packetType.getName() + " does not match direction " + direction);
    }
  }

  @SuppressWarnings("unchecked")
  private static <P extends ClientboundPacket> RegisteredPacketCodec<P> castRegistration(
      RegisteredPacketCodec<?> registration) {
    return (RegisteredPacketCodec<P>) registration;
  }

  private record CodecKey(
      ProtocolProfileId profileId,
      ProtocolPhase phase,
      PacketDirection direction,
      PacketId packetId) {}

  private record EncoderKey(
      ProtocolProfileId profileId,
      ProtocolPhase phase,
      Class<? extends ProtocolPacket> packetType) {}
}
