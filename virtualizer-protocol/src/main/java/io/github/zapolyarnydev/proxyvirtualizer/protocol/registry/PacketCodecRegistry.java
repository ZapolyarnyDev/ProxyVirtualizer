package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.codec.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.PacketDirection;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ProtocolPacket;
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

  public <P extends ProtocolPacket> void register(ProtocolVersion version, PacketCodec<P> codec) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(codec, "codec");
    profiles.require(version);
    CodecKey key = new CodecKey(version, codec.direction(), codec.packetId());
    if (codecs.putIfAbsent(key, codec) != null) {
      throw new DuplicatePacketCodecException("Packet codec is already registered: " + key);
    }
  }

  public Optional<PacketCodec<?>> find(
      ProtocolVersion version, PacketDirection direction, PacketId packetId) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(direction, "direction");
    Objects.requireNonNull(packetId, "packetId");
    profiles.require(version);
    return Optional.ofNullable(codecs.get(new CodecKey(version, direction, packetId)));
  }

  private record CodecKey(ProtocolVersion version, PacketDirection direction, PacketId packetId) {}
}
