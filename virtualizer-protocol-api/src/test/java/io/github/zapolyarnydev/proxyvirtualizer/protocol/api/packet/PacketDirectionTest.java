package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class PacketDirectionTest {

  @Test
  void assignsDirectionFromPacketMarker() {
    ClientboundPacket clientboundPacket = new ClientboundPacket() {};
    ServerboundPacket serverboundPacket = new ServerboundPacket() {};

    assertThat(clientboundPacket.direction()).isEqualTo(PacketDirection.CLIENTBOUND);
    assertThat(serverboundPacket.direction()).isEqualTo(PacketDirection.SERVERBOUND);
  }
}
