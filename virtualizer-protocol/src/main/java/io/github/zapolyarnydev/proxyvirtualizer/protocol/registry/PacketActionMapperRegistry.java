package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.PacketActionMapper;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception.DuplicatePacketActionMapperException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PacketActionMapperRegistry {

  private final ProtocolProfileRegistry profiles;
  private final Map<MapperKey, PacketActionMapper<?>> mappers = new HashMap<>();

  PacketActionMapperRegistry(ProtocolProfileRegistry profiles) {
    this.profiles = Objects.requireNonNull(profiles, "profiles");
  }

  public synchronized <P extends ServerboundPacket> void register(
      ProtocolProfileId profileId,
      ProtocolPhase phase,
      Class<P> packetType,
      PacketActionMapper<P> mapper) {
    Objects.requireNonNull(profileId, "profileId");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(packetType, "packetType");
    Objects.requireNonNull(mapper, "mapper");
    profiles.require(profileId, phase);
    MapperKey key = new MapperKey(profileId, phase, packetType);
    if (mappers.putIfAbsent(key, mapper) != null) {
      throw new DuplicatePacketActionMapperException(
          "Packet action mapper is already registered: " + key);
    }
  }

  public <P extends ServerboundPacket> Optional<PacketActionMapper<P>> find(
      ProtocolVersion version, ProtocolPhase phase, Class<P> packetType) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(packetType, "packetType");
    ProtocolProfileId profileId = profiles.require(version, phase).id();
    return Optional.ofNullable(
        castMapper(mappers.get(new MapperKey(profileId, phase, packetType))));
  }

  @SuppressWarnings("unchecked")
  private static <P extends ServerboundPacket> PacketActionMapper<P> castMapper(
      PacketActionMapper<?> mapper) {
    return (PacketActionMapper<P>) mapper;
  }

  private record MapperKey(
      ProtocolProfileId profileId,
      ProtocolPhase phase,
      Class<? extends ServerboundPacket> packetType) {}
}
