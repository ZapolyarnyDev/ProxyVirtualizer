package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketDirection;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class Minecraft26_2TransitionSequenceTest {

  @Test
  void pinsThe776ActivePlayToVirtualPlayConfigurationSequence() {
    ProtocolRegistry registry = new ProtocolRegistry();
    Minecraft26_2Protocol.install(registry);

    assertThat(Minecraft26_2Protocol.START_CONFIGURATION_ID.value()).isEqualTo(0x76);
    assertThat(Minecraft26_2Protocol.CONFIGURATION_ACKNOWLEDGED_ID.value()).isEqualTo(0x10);
    assertThat(Minecraft26_2Protocol.FINISH_CONFIGURATION_ID.value()).isEqualTo(0x03);
    assertThat(Minecraft26_2Protocol.REGISTRY_DATA_ID.value()).isEqualTo(0x07);
    assertThat(Minecraft26_2Protocol.FEATURE_FLAGS_ID.value()).isEqualTo(0x0C);
    assertThat(Minecraft26_2Protocol.SELECT_KNOWN_PACKS_ID.value()).isEqualTo(0x0E);
    assertThat(Minecraft26_2Protocol.KNOWN_PACKS_ID.value()).isEqualTo(0x07);
    assertThat(Minecraft26_2Protocol.PLAY_LOGIN_ID.value()).isEqualTo(0x31);
    assertThat(Minecraft26_2Protocol.SYNCHRONIZE_PLAYER_POSITION_ID.value()).isEqualTo(0x48);
    assertThat(
            registry
                .profiles()
                .require(Minecraft26_2Protocol.VERSION, ProtocolPhase.CONFIGURATION)
                .supports(Minecraft26_2Protocol.VIRTUAL_PLAY_TRANSITION))
        .isTrue();
    assertThat(
            registry
                .codecs()
                .find(
                    Minecraft26_2Protocol.VERSION,
                    ProtocolPhase.PLAY,
                    PacketDirection.CLIENTBOUND,
                    Minecraft26_2Protocol.START_CONFIGURATION_ID))
        .isPresent();
    assertThat(
            registry
                .codecs()
                .find(
                    Minecraft26_2Protocol.VERSION,
                    ProtocolPhase.CONFIGURATION,
                    PacketDirection.SERVERBOUND,
                    Minecraft26_2Protocol.KNOWN_PACKS_ID))
        .isPresent();
  }

  @Test
  void encodesThe776TeleportAndChunkViewPayloads() {
    SynchronizePlayerPositionCodec teleportCodec = new SynchronizePlayerPositionCodec();
    ByteBuffer teleport =
        teleportCodec.encode(new SynchronizePlayerPositionPacket(128, 1, 2, 3, 0, 0, 0, 90, 0, 0));
    assertThat(teleport.get()).isEqualTo((byte) 0x80);
    assertThat(teleport.get()).isEqualTo((byte) 0x01);
    teleport.rewind();
    assertThat(teleportCodec.decode(teleport))
        .isEqualTo(new SynchronizePlayerPositionPacket(128, 1, 2, 3, 0, 0, 0, 90, 0, 0));
    assertThat(
            new SetChunkCacheCenterCodec()
                .decode(
                    new SetChunkCacheCenterCodec().encode(new SetChunkCacheCenterPacket(-1, 4))))
        .isEqualTo(new SetChunkCacheCenterPacket(-1, 4));
  }

  @Test
  void roundTripsThe776LoginAndRespawnSchema() {
    PlayLoginPacket.DimensionState dimension =
        new PlayLoginPacket.DimensionState(
            0,
            "minecraft:overworld",
            42L,
            (byte) 1,
            (byte) -1,
            false,
            true,
            Optional.of(new PlayLoginPacket.DeathLocation("minecraft:overworld", 7L)),
            0,
            63);
    PlayLoginPacket login =
        new PlayLoginPacket(
            12,
            false,
            List.of("minecraft:overworld"),
            20,
            10,
            10,
            false,
            true,
            false,
            dimension,
            true,
            true);

    assertThat(new PlayLoginCodec().decode(new PlayLoginCodec().encode(login))).isEqualTo(login);
    assertThat(
            new RespawnCodec()
                .decode(new RespawnCodec().encode(new RespawnPacket(dimension, (byte) 3))))
        .isEqualTo(new RespawnPacket(dimension, (byte) 3));
  }
}
