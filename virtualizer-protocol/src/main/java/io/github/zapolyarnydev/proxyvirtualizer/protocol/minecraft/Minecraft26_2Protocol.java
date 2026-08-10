package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

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
  public static final ProtocolCapability INBOUND_KEEP_ALIVE =
      ProtocolCapability.of("proxyvirtualizer:inbound-keep-alive");
  public static final PacketId SERVERBOUND_KEEP_ALIVE_ID = new PacketId(0x1C);

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
  }

  private record Profile(
      ProtocolProfileId id,
      Set<ProtocolVersion> versions,
      Set<ProtocolPhase> phases,
      Set<ProtocolCapability> capabilities)
      implements ProtocolProfile {

    private Profile() {
      this(PROFILE_ID, Set.of(VERSION), Set.of(ProtocolPhase.PLAY), Set.of(INBOUND_KEEP_ALIVE));
    }
  }
}
