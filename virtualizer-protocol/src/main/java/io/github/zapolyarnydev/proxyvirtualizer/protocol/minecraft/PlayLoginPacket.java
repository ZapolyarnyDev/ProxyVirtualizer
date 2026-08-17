package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PlayLoginPacket(
    int entityId,
    boolean hardcore,
    List<String> dimensionNames,
    int maxPlayers,
    int viewDistance,
    int simulationDistance,
    boolean reducedDebugInfo,
    boolean respawnScreenEnabled,
    boolean limitedCrafting,
    DimensionState dimension,
    boolean onlineMode,
    boolean enforcesSecureChat)
    implements ClientboundPacket {

  public PlayLoginPacket {
    dimensionNames = List.copyOf(Objects.requireNonNull(dimensionNames, "dimensionNames"));
    dimension = Objects.requireNonNull(dimension, "dimension");
  }

  public record DimensionState(
      int typeId,
      String name,
      long hashedSeed,
      byte gameMode,
      byte previousGameMode,
      boolean debug,
      boolean flat,
      Optional<DeathLocation> deathLocation,
      int portalCooldown,
      int seaLevel) {
    public DimensionState {
      name = Objects.requireNonNull(name, "name");
      deathLocation = Objects.requireNonNull(deathLocation, "deathLocation");
    }
  }

  public record DeathLocation(String dimensionName, long packedPosition) {
    public DeathLocation {
      dimensionName = Objects.requireNonNull(dimensionName, "dimensionName");
    }
  }
}
