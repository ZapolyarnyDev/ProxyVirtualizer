package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.codec.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.PacketDirection;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ProtocolPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.DuplicatePacketCodecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PacketCodecRegistry {

  private final ProtocolProfileRegistry profiles;
  private final Map<CodecKey, PacketCodec<?>> codecs = new HashMap<>();

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

  private <P extends ProtocolPacket> void register(
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
    profiles.require(profileId, phase);
    CodecKey key = new CodecKey(profileId, phase, direction, packetId);
    if (codecs.putIfAbsent(key, codec) != null) {
      throw new DuplicatePacketCodecException("Packet codec is already registered: " + key);
    }
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

  private record CodecKey(
      ProtocolProfileId profileId,
      ProtocolPhase phase,
      PacketDirection direction,
      PacketId packetId) {}
}
