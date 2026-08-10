package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolCapability;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
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
          assertThat(receivedContext.profile().versions().contains(new ProtocolVersion(769)))
              .isTrue();
          assertThat(receivedContext.phase()).isEqualTo(ProtocolPhase.PLAY);
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
        public ProtocolProfileId id() {
          return new ProtocolProfileId("test");
        }

        @Override
        public Set<ProtocolVersion> versions() {
          return Set.of(new ProtocolVersion(769));
        }

        @Override
        public Set<ProtocolPhase> phases() {
          return Set.of(ProtocolPhase.PLAY);
        }

        @Override
        public Set<ProtocolCapability> capabilities() {
          return Set.of();
        }
      };
    }

    @Override
    public ProtocolPhase phase() {
      return ProtocolPhase.PLAY;
    }

    @Override
    public void send(ClientboundPacket packet) {
      sentPackets.add(packet);
    }
  }
}
