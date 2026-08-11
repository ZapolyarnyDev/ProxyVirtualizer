package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.SemanticAction;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolContext;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolCapability;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.PacketActionMappingException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.SemanticActionHandlingException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.UnknownPacketActionMapperException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ProtocolSessionProcessorTest {

  private static final ProtocolVersion VERSION = ProtocolVersion.of(776);
  private static final ProtocolProfileId PROFILE_ID = ProtocolProfileId.of("test:776");
  private static final PacketId PACKET_ID = new PacketId(7);

  private ProtocolRegistry registry;
  private ProtocolSessionProcessor processor;
  private ProtocolContext context;

  @BeforeEach
  void setUp() {
    registry = new ProtocolRegistry();
    ProtocolProfile profile =
        new TestProfile(PROFILE_ID, Set.of(VERSION), Set.of(ProtocolPhase.PLAY), Set.of());
    registry.profiles().register(profile);
    registry
        .codecs()
        .registerServerbound(PROFILE_ID, ProtocolPhase.PLAY, PACKET_ID, new TestPacketCodec());
    processor = new ProtocolSessionProcessor(registry);
    context = new TestContext(VERSION, profile, ProtocolPhase.PLAY);
  }

  @Test
  void decodesMapsAndDispatchesActionsInOrder() {
    registry
        .actionMappers()
        .register(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            TestPacket.class,
            (receivedContext, packet, actions) -> {
              assertThat(receivedContext).isSameAs(context);
              actions.accept(new TestAction("first"));
              actions.accept(new TestAction("second"));
            });
    List<SemanticAction> received = new ArrayList<>();

    processor.process(context, emptyFrame(), received::add);

    assertThat(received).containsExactly(new TestAction("first"), new TestAction("second"));
  }

  @Test
  void rejectsPacketWithoutActionMapper() {
    assertThatThrownBy(() -> processor.process(context, emptyFrame(), ignored -> {}))
        .isInstanceOf(UnknownPacketActionMapperException.class)
        .hasMessageContaining(TestPacket.class.getName());
  }

  @Test
  void doesNotDispatchPartialActionsWhenMapperFails() {
    registry
        .actionMappers()
        .register(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            TestPacket.class,
            (receivedContext, packet, actions) -> {
              actions.accept(new TestAction("must not escape"));
              throw new IllegalStateException("mapping failed");
            });
    List<SemanticAction> received = new ArrayList<>();

    assertThatThrownBy(() -> processor.process(context, emptyFrame(), received::add))
        .isInstanceOf(PacketActionMappingException.class)
        .hasRootCauseMessage("mapping failed");
    assertThat(received).isEmpty();
  }

  @Test
  void distinguishesActionHandlingFailureFromMappingFailure() {
    registry
        .actionMappers()
        .register(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            TestPacket.class,
            (receivedContext, packet, actions) -> actions.accept(new TestAction("action")));

    assertThatThrownBy(
            () ->
                processor.process(
                    context,
                    emptyFrame(),
                    action -> {
                      throw new IllegalStateException("handler failed");
                    }))
        .isInstanceOf(SemanticActionHandlingException.class)
        .hasRootCauseMessage("handler failed");
  }

  private static InboundFrame emptyFrame() {
    return new InboundFrame(PACKET_ID, ByteBuffer.allocate(0));
  }

  private record TestPacket() implements ServerboundPacket {}

  private record TestAction(String value) implements SemanticAction {}

  private record TestProfile(
      ProtocolProfileId id,
      Set<ProtocolVersion> versions,
      Set<ProtocolPhase> phases,
      Set<ProtocolCapability> capabilities)
      implements ProtocolProfile {}

  private record TestContext(ProtocolVersion version, ProtocolProfile profile, ProtocolPhase phase)
      implements ProtocolContext {

    @Override
    public void send(ClientboundPacket packet) {}
  }

  private static final class TestPacketCodec implements PacketCodec<TestPacket> {

    @Override
    public @NotNull Class<TestPacket> packetType() {
      return TestPacket.class;
    }

    @Override
    public @NotNull TestPacket decode(@NotNull ByteBuffer input) {
      return new TestPacket();
    }

    @Override
    public @NotNull ByteBuffer encode(@NotNull TestPacket packet) {
      return ByteBuffer.allocate(0);
    }
  }
}
