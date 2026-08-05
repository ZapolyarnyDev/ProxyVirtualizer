package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model;

public interface ClientboundPacket extends ProtocolPacket {

  @Override
  default PacketDirection direction() {
    return PacketDirection.CLIENTBOUND;
  }
}
