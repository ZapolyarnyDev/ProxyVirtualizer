package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.handler;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolCapability;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.context.ProtocolContext;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class PacketHandlerTest {

  @Test
  void receivesContextAndCanSendClientboundPacket() {
    List<ClientboundPacket> sentPackets = new ArrayList<>();
    ProtocolContext context = new TestProtocolContext(sentPackets);
    PacketHandler<TestServerboundPacket> handler =
        (receivedContext, packet) -> {
          assertThat(receivedContext.profile().version()).isEqualTo(new ProtocolVersion(769));
          receivedContext.send(new TestClientboundPacket());
        };

    handler.handle(context, new TestServerboundPacket());

    assertThat(sentPackets).containsExactly(new TestClientboundPacket());
  }

  private record TestServerboundPacket() implements ServerboundPacket {}

  private record TestClientboundPacket() implements ClientboundPacket {}

  private record TestProtocolContext(List<ClientboundPacket> sentPackets)
      implements ProtocolContext {

    @Override
    public ProtocolProfile profile() {
      return new ProtocolProfile() {
        @Override
        public ProtocolVersion version() {
          return new ProtocolVersion(769);
        }

        @Override
        public Set<ProtocolCapability> capabilities() {
          return Set.of();
        }
      };
    }

    @Override
    public void send(ClientboundPacket packet) {
      sentPackets.add(packet);
    }
  }
}
