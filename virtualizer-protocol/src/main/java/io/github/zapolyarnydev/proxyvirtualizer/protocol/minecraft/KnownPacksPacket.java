package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ServerboundPacket;
import java.util.List;
import java.util.Objects;

public record KnownPacksPacket(List<KnownPack> packs) implements ServerboundPacket {
  public KnownPacksPacket {
    packs = List.copyOf(Objects.requireNonNull(packs, "packs"));
  }
}
