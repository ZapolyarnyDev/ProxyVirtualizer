package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model;

public interface ServerboundPacket extends ProtocolPacket {

  @Override
  default PacketDirection direction() {
    return PacketDirection.SERVERBOUND;
  }
}
