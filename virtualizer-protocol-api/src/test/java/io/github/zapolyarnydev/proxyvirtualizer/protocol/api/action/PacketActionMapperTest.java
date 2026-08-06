package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.context.ProtocolContext;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolCapability;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersionRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class PacketActionMapperTest {

  @Test
  void canPublishMultipleSemanticActions() {
    PacketActionMapper<TestPacket> mapper =
        (context, packet, actions) -> {
          actions.accept(new FirstAction());
          actions.accept(new SecondAction());
        };
    List<SemanticAction> actions = new ArrayList<>();

    mapper.map(new TestProtocolContext(), new TestPacket(), actions::add);

    assertThat(actions).containsExactly(new FirstAction(), new SecondAction());
  }

  private record TestPacket() implements ServerboundPacket {}

  private record FirstAction() implements SemanticAction {}

  private record SecondAction() implements SemanticAction {}

  private static final class TestProtocolContext implements ProtocolContext {

    @Override
    public ProtocolProfile profile() {
      return new ProtocolProfile() {
        @Override
        public ProtocolProfileId id() {
          return new ProtocolProfileId("test");
        }

        @Override
        public ProtocolVersionRange versions() {
          return ProtocolVersionRange.exact(new ProtocolVersion(769));
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
    public void send(ClientboundPacket packet) {}
  }
}
