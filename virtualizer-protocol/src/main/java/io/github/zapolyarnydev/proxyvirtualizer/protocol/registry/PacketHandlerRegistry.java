package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.handler.PacketHandler;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.DuplicatePacketHandlerException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PacketHandlerRegistry {

  private final ProtocolProfileRegistry profiles;
  private final Map<HandlerKey, PacketHandler<?>> handlers = new HashMap<>();

  PacketHandlerRegistry(ProtocolProfileRegistry profiles) {
    this.profiles = Objects.requireNonNull(profiles, "profiles");
  }

  public <P extends ServerboundPacket> void register(
      ProtocolProfileId profileId,
      ProtocolPhase phase,
      Class<P> packetType,
      PacketHandler<P> handler) {
    Objects.requireNonNull(profileId, "profileId");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(packetType, "packetType");
    Objects.requireNonNull(handler, "handler");
    profiles.require(profileId, phase);
    HandlerKey key = new HandlerKey(profileId, phase, packetType);
    if (handlers.putIfAbsent(key, handler) != null) {
      throw new DuplicatePacketHandlerException("Packet handler is already registered: " + key);
    }
  }

  public <P extends ServerboundPacket> Optional<PacketHandler<P>> find(
      ProtocolVersion version, ProtocolPhase phase, Class<P> packetType) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(packetType, "packetType");
    ProtocolProfileId profileId = profiles.require(version, phase).id();
    return Optional.ofNullable(
        castHandler(handlers.get(new HandlerKey(profileId, phase, packetType))));
  }

  @SuppressWarnings("unchecked")
  private static <P extends ServerboundPacket> PacketHandler<P> castHandler(
      PacketHandler<?> handler) {
    return (PacketHandler<P>) handler;
  }

  private record HandlerKey(
      ProtocolProfileId profileId,
      ProtocolPhase phase,
      Class<? extends ServerboundPacket> packetType) {}
}
