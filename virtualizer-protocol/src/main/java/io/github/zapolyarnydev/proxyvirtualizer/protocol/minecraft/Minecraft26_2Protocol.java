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
  public static final PacketId SERVERBOUND_KEEP_ALIVE_ID = new PacketId(0x1C);
  public static final PacketId CLIENTBOUND_KEEP_ALIVE_ID = new PacketId(0x2C);

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
  }

  private record Profile(
      ProtocolProfileId id,
      Set<ProtocolVersion> versions,
      Set<ProtocolPhase> phases,
      Set<ProtocolCapability> capabilities)
      implements ProtocolProfile {

    private Profile() {
      this(PROFILE_ID, Set.of(VERSION), Set.of(ProtocolPhase.PLAY), Set.of(KEEP_ALIVE));
    }
  }
}
