package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.KeepAliveAcknowledged;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolCapability;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import java.util.Objects;
import java.util.Set;

public final class Minecraft26_2Protocol {

  public static final ProtocolVersion VERSION = ProtocolVersion.of(776);
  public static final ProtocolProfileId PROFILE_ID = ProtocolProfileId.of("minecraft:26.2");
  public static final ProtocolCapability KEEP_ALIVE =
      ProtocolCapability.of("proxyvirtualizer:keep-alive");
  public static final ProtocolCapability VIRTUAL_PLAY_TRANSITION =
      ProtocolCapability.of("proxyvirtualizer:virtual-play-transition");
  public static final PacketId SERVERBOUND_KEEP_ALIVE_ID = new PacketId(0x1C);
  public static final PacketId CLIENTBOUND_KEEP_ALIVE_ID = new PacketId(0x2C);
  public static final PacketId START_CONFIGURATION_ID = new PacketId(0x76);
  public static final PacketId CONFIGURATION_ACKNOWLEDGED_ID = new PacketId(0x10);
  public static final PacketId FINISH_CONFIGURATION_ID = new PacketId(0x03);
  public static final PacketId REGISTRY_DATA_ID = new PacketId(0x07);
  public static final PacketId FEATURE_FLAGS_ID = new PacketId(0x0C);
  public static final PacketId SELECT_KNOWN_PACKS_ID = new PacketId(0x0E);
  public static final PacketId KNOWN_PACKS_ID = new PacketId(0x07);
  public static final PacketId PLAY_LOGIN_ID = new PacketId(0x31);
  public static final PacketId RESPAWN_ID = new PacketId(0x52);
  public static final PacketId SYNCHRONIZE_PLAYER_POSITION_ID = new PacketId(0x48);
  public static final PacketId SET_CHUNK_CACHE_CENTER_ID = new PacketId(0x5E);

  private Minecraft26_2Protocol() {}

  public static void install(ProtocolRegistry registry) {
    Objects.requireNonNull(registry, "registry");
    registry.profiles().register(new Profile());
    registry
        .codecs()
        .registerServerbound(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            SERVERBOUND_KEEP_ALIVE_ID,
            new ServerboundKeepAliveCodec());
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            CLIENTBOUND_KEEP_ALIVE_ID,
            new ClientboundKeepAliveCodec());
    registry
        .actionMappers()
        .register(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            ServerboundKeepAlivePacket.class,
            (context, packet, actions) -> actions.accept(new KeepAliveAcknowledged(packet.id())));
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            START_CONFIGURATION_ID,
            new EmptyPacketCodec<>(StartConfigurationPacket.class, StartConfigurationPacket::new));
    registry
        .codecs()
        .registerServerbound(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            CONFIGURATION_ACKNOWLEDGED_ID,
            new EmptyPacketCodec<>(
                ConfigurationAcknowledgedPacket.class, ConfigurationAcknowledgedPacket::new));
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID,
            ProtocolPhase.CONFIGURATION,
            FINISH_CONFIGURATION_ID,
            new EmptyPacketCodec<>(
                FinishConfigurationPacket.class, FinishConfigurationPacket::new));
    registry
        .codecs()
        .registerServerbound(
            PROFILE_ID,
            ProtocolPhase.CONFIGURATION,
            FINISH_CONFIGURATION_ID,
            new EmptyPacketCodec<>(
                FinishConfigurationAcknowledgedPacket.class,
                FinishConfigurationAcknowledgedPacket::new));
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID, ProtocolPhase.CONFIGURATION, REGISTRY_DATA_ID, new RegistryDataCodec());
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID, ProtocolPhase.CONFIGURATION, FEATURE_FLAGS_ID, new FeatureFlagsCodec());
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID,
            ProtocolPhase.CONFIGURATION,
            SELECT_KNOWN_PACKS_ID,
            new KnownPacksCodec<>(
                SelectKnownPacksPacket.class,
                SelectKnownPacksPacket::new,
                SelectKnownPacksPacket::packs));
    registry
        .codecs()
        .registerServerbound(
            PROFILE_ID,
            ProtocolPhase.CONFIGURATION,
            KNOWN_PACKS_ID,
            new KnownPacksCodec<>(
                KnownPacksPacket.class, KnownPacksPacket::new, KnownPacksPacket::packs));
    registry
        .codecs()
        .registerClientbound(PROFILE_ID, ProtocolPhase.PLAY, PLAY_LOGIN_ID, new PlayLoginCodec());
    registry
        .codecs()
        .registerClientbound(PROFILE_ID, ProtocolPhase.PLAY, RESPAWN_ID, new RespawnCodec());
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            SYNCHRONIZE_PLAYER_POSITION_ID,
            new SynchronizePlayerPositionCodec());
    registry
        .codecs()
        .registerClientbound(
            PROFILE_ID,
            ProtocolPhase.PLAY,
            SET_CHUNK_CACHE_CENTER_ID,
            new SetChunkCacheCenterCodec());
  }

  private record Profile(
      ProtocolProfileId id,
      Set<ProtocolVersion> versions,
      Set<ProtocolPhase> phases,
      Set<ProtocolCapability> capabilities)
      implements ProtocolProfile {

    private Profile() {
      this(
          PROFILE_ID,
          Set.of(VERSION),
          Set.of(ProtocolPhase.CONFIGURATION, ProtocolPhase.PLAY),
          Set.of(KEEP_ALIVE, VIRTUAL_PLAY_TRANSITION));
    }
  }
}
