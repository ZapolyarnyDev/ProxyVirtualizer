package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import java.util.List;
import java.util.Objects;

public record SelectKnownPacksPacket(List<KnownPack> packs) implements ClientboundPacket {
  public SelectKnownPacksPacket {
    packs = List.copyOf(Objects.requireNonNull(packs, "packs"));
  }
}
