package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id;

public record PacketId(int value) {

  public PacketId {
    if (value < 0) {
      throw new IllegalArgumentException("Packet id must not be negative");
    }
  }
}
