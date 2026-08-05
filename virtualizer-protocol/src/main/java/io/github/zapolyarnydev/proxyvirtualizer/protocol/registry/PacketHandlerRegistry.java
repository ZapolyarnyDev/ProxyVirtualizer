package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.handler.PacketHandler;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;
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
      ProtocolVersion version, Class<P> packetType, PacketHandler<P> handler) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(packetType, "packetType");
    Objects.requireNonNull(handler, "handler");
    profiles.require(version);
    HandlerKey key = new HandlerKey(version, packetType);
    if (handlers.putIfAbsent(key, handler) != null) {
      throw new DuplicatePacketHandlerException("Packet handler is already registered: " + key);
    }
  }

  public <P extends ServerboundPacket> Optional<PacketHandler<P>> find(
      ProtocolVersion version, Class<P> packetType) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(packetType, "packetType");
    profiles.require(version);
    return Optional.ofNullable(castHandler(handlers.get(new HandlerKey(version, packetType))));
  }

  @SuppressWarnings("unchecked")
  private static <P extends ServerboundPacket> PacketHandler<P> castHandler(
      PacketHandler<?> handler) {
    return (PacketHandler<P>) handler;
  }

  private record HandlerKey(
      ProtocolVersion version, Class<? extends ServerboundPacket> packetType) {}
}
