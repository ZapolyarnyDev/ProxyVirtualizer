package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.codec.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.handler.PacketHandler;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.PacketDirection;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolCapability;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.DuplicatePacketCodecException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.DuplicatePacketHandlerException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.DuplicateProtocolProfileException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.DuplicateProtocolVersionException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.UnsupportedProtocolPhaseException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.UnsupportedProtocolVersionException;
import java.nio.ByteBuffer;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ProtocolRegistryTest {

  private static final ProtocolVersion VERSION = new ProtocolVersion(769);
  private static final ProtocolVersion COMPATIBLE_VERSION = new ProtocolVersion(770);
  private static final ProtocolProfileId PROFILE_ID = new ProtocolProfileId("minecraft-1.21");
  private static final ProtocolCapability VIRTUAL_SESSION =
      new ProtocolCapability("virtual-session");

  private ProtocolRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new ProtocolRegistry();
    registry
        .profiles()
        .register(
            new TestProtocolProfile(
                PROFILE_ID,
                Set.of(VERSION, COMPATIBLE_VERSION),
                Set.of(ProtocolPhase.LOGIN, ProtocolPhase.PLAY),
                Set.of(VIRTUAL_SESSION)));
  }

  @Test
  void rejectsDuplicateProtocolProfile() {
    assertThatThrownBy(
            () ->
                registry
                    .profiles()
                    .register(
                        new TestProtocolProfile(
                            PROFILE_ID,
                            Set.of(VERSION),
                            Set.of(ProtocolPhase.PLAY),
                            Set.of(VIRTUAL_SESSION))))
        .isInstanceOf(DuplicateProtocolProfileException.class);
  }

  @Test
  void rejectsProtocolVersionClaimedByAnotherProfile() {
    assertThatThrownBy(
            () ->
                registry
                    .profiles()
                    .register(
                        new TestProtocolProfile(
                            new ProtocolProfileId("minecraft-overlap"),
                            Set.of(COMPATIBLE_VERSION, new ProtocolVersion(771)),
                            Set.of(ProtocolPhase.PLAY),
                            Set.of())))
        .isInstanceOf(DuplicateProtocolVersionException.class);
  }

  @Test
  void rejectsUnsupportedProtocolVersion() {
    ProtocolVersion unsupportedVersion = new ProtocolVersion(771);

    assertThatThrownBy(() -> registry.profiles().require(unsupportedVersion))
        .isInstanceOf(UnsupportedProtocolVersionException.class);
    assertThat(registry.profiles().supports(unsupportedVersion)).isFalse();
  }

  @Test
  void resolvesProtocolCapabilities() {
    assertThat(registry.profiles().supports(VERSION, VIRTUAL_SESSION)).isTrue();
    assertThat(registry.profiles().supports(VERSION, new ProtocolCapability("keep-alive")))
        .isFalse();
  }

  @Test
  void resolvesProtocolPhaseSupport() {
    assertThat(registry.profiles().supports(COMPATIBLE_VERSION, ProtocolPhase.PLAY)).isTrue();
    assertThat(registry.profiles().supports(VERSION, ProtocolPhase.CONFIGURATION)).isFalse();
    assertThat(registry.profiles().supports(new ProtocolVersion(771), ProtocolPhase.PLAY))
        .isFalse();
  }

  @Test
  void resolvesCodecByVersionDirectionAndPacketId() {
    PacketCodec<TestClientboundPacket> codec = new TestClientboundCodec();
    registry.codecs().registerClientbound(PROFILE_ID, ProtocolPhase.PLAY, new PacketId(42), codec);

    assertThat(
            registry
                .codecs()
                .find(
                    COMPATIBLE_VERSION,
                    ProtocolPhase.PLAY,
                    PacketDirection.CLIENTBOUND,
                    new PacketId(42)))
        .containsSame(codec);
  }

  @Test
  void resolvesSamePacketIdInDifferentPhases() {
    PacketCodec<TestClientboundPacket> loginCodec = new TestClientboundCodec();
    PacketCodec<TestClientboundPacket> playCodec = new TestClientboundCodec();
    registry
        .codecs()
        .registerClientbound(PROFILE_ID, ProtocolPhase.LOGIN, new PacketId(42), loginCodec);
    registry
        .codecs()
        .registerClientbound(PROFILE_ID, ProtocolPhase.PLAY, new PacketId(42), playCodec);

    assertThat(
            registry
                .codecs()
                .find(VERSION, ProtocolPhase.LOGIN, PacketDirection.CLIENTBOUND, new PacketId(42)))
        .containsSame(loginCodec);
    assertThat(
            registry
                .codecs()
                .find(VERSION, ProtocolPhase.PLAY, PacketDirection.CLIENTBOUND, new PacketId(42)))
        .containsSame(playCodec);
  }

  @Test
  void rejectsDuplicateCodecRegistration() {
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID, ProtocolPhase.PLAY, new PacketId(42), new TestClientboundCodec());

    assertThatThrownBy(
            () ->
                registry
                    .codecs()
                    .registerClientbound(
                        PROFILE_ID,
                        ProtocolPhase.PLAY,
                        new PacketId(42),
                        new TestClientboundCodec()))
        .isInstanceOf(DuplicatePacketCodecException.class);
  }

  @Test
  void rejectsCodecRegistrationForUnsupportedPhase() {
    assertThatThrownBy(
            () ->
                registry
                    .codecs()
                    .registerClientbound(
                        PROFILE_ID,
                        ProtocolPhase.CONFIGURATION,
                        new PacketId(42),
                        new TestClientboundCodec()))
        .isInstanceOf(UnsupportedProtocolPhaseException.class);
  }

  @Test
  void resolvesHandlerByVersionAndPacketType() {
    PacketHandler<TestServerboundPacket> handler = (context, packet) -> {};
    registry
        .handlers()
        .register(PROFILE_ID, ProtocolPhase.PLAY, TestServerboundPacket.class, handler);

    assertThat(
            registry
                .handlers()
                .find(COMPATIBLE_VERSION, ProtocolPhase.PLAY, TestServerboundPacket.class))
        .containsSame(handler);
  }

  @Test
  void rejectsDuplicateHandlerRegistration() {
    registry
        .handlers()
        .register(
            PROFILE_ID, ProtocolPhase.PLAY, TestServerboundPacket.class, (context, packet) -> {});

    assertThatThrownBy(
            () ->
                registry
                    .handlers()
                    .register(
                        PROFILE_ID,
                        ProtocolPhase.PLAY,
                        TestServerboundPacket.class,
                        (context, packet) -> {}))
        .isInstanceOf(DuplicatePacketHandlerException.class);
  }

  private record TestProtocolProfile(
      ProtocolProfileId id,
      Set<ProtocolVersion> versions,
      Set<ProtocolPhase> phases,
      Set<ProtocolCapability> capabilities)
      implements ProtocolProfile {}

  private record TestClientboundPacket() implements ClientboundPacket {}

  private record TestServerboundPacket() implements ServerboundPacket {}

  private static final class TestClientboundCodec implements PacketCodec<TestClientboundPacket> {

    @Override
    public @NotNull Class<TestClientboundPacket> packetType() {
      return TestClientboundPacket.class;
    }

    @Override
    public @NotNull TestClientboundPacket decode(@NotNull ByteBuffer input) {
      return new TestClientboundPacket();
    }

    @Override
    public void encode(@NotNull TestClientboundPacket packet, @NotNull ByteBuffer output) {}
  }
}
