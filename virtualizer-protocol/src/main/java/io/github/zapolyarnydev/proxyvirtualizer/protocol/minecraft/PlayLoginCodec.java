package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class PlayLoginCodec implements PacketCodec<PlayLoginPacket> {

  @Override
  public @NotNull Class<PlayLoginPacket> packetType() {
    return PlayLoginPacket.class;
  }

  @Override
  public @NotNull PlayLoginPacket decode(@NotNull ByteBuffer input) {
    int entityId = input.getInt();
    boolean hardcore = input.get() != 0;
    var dimensions = MinecraftWire.readStringList(input);
    int maxPlayers = MinecraftWire.readVarInt(input);
    int viewDistance = MinecraftWire.readVarInt(input);
    int simulationDistance = MinecraftWire.readVarInt(input);
    boolean reducedDebugInfo = input.get() != 0;
    boolean respawnScreenEnabled = input.get() != 0;
    boolean limitedCrafting = input.get() != 0;
    PlayLoginPacket.DimensionState dimension = readDimension(input);
    return new PlayLoginPacket(
        entityId,
        hardcore,
        dimensions,
        maxPlayers,
        viewDistance,
        simulationDistance,
        reducedDebugInfo,
        respawnScreenEnabled,
        limitedCrafting,
        dimension,
        input.get() != 0,
        input.get() != 0);
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull PlayLoginPacket packet) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes(
        ByteBuffer.allocate(5)
            .putInt(packet.entityId())
            .put((byte) (packet.hardcore() ? 1 : 0))
            .array());
    MinecraftWire.writeStringList(output, packet.dimensionNames());
    MinecraftWire.writeVarInt(output, packet.maxPlayers());
    MinecraftWire.writeVarInt(output, packet.viewDistance());
    MinecraftWire.writeVarInt(output, packet.simulationDistance());
    output.write(packet.reducedDebugInfo() ? 1 : 0);
    output.write(packet.respawnScreenEnabled() ? 1 : 0);
    output.write(packet.limitedCrafting() ? 1 : 0);
    writeDimension(output, packet.dimension());
    output.write(packet.onlineMode() ? 1 : 0);
    output.write(packet.enforcesSecureChat() ? 1 : 0);
    return MinecraftWire.buffer(output);
  }

  static PlayLoginPacket.DimensionState readDimension(ByteBuffer input) {
    int typeId = MinecraftWire.readVarInt(input);
    String name = MinecraftWire.readString(input);
    long hashedSeed = input.getLong();
    byte gameMode = input.get();
    byte previousGameMode = input.get();
    boolean debug = input.get() != 0;
    boolean flat = input.get() != 0;
    Optional<PlayLoginPacket.DeathLocation> deathLocation = Optional.empty();
    if (input.get() != 0)
      deathLocation =
          Optional.of(
              new PlayLoginPacket.DeathLocation(MinecraftWire.readString(input), input.getLong()));
    return new PlayLoginPacket.DimensionState(
        typeId,
        name,
        hashedSeed,
        gameMode,
        previousGameMode,
        debug,
        flat,
        deathLocation,
        MinecraftWire.readVarInt(input),
        MinecraftWire.readVarInt(input));
  }

  static void writeDimension(
      ByteArrayOutputStream output, PlayLoginPacket.DimensionState dimension) {
    MinecraftWire.writeVarInt(output, dimension.typeId());
    MinecraftWire.writeString(output, dimension.name());
    output.writeBytes(
        ByteBuffer.allocate(12)
            .putLong(dimension.hashedSeed())
            .put(dimension.gameMode())
            .put(dimension.previousGameMode())
            .put((byte) (dimension.debug() ? 1 : 0))
            .put((byte) (dimension.flat() ? 1 : 0))
            .array());
    output.write(dimension.deathLocation().isPresent() ? 1 : 0);
    dimension
        .deathLocation()
        .ifPresent(
            location -> {
              MinecraftWire.writeString(output, location.dimensionName());
              output.writeBytes(
                  ByteBuffer.allocate(Long.BYTES).putLong(location.packedPosition()).array());
            });
    MinecraftWire.writeVarInt(output, dimension.portalCooldown());
    MinecraftWire.writeVarInt(output, dimension.seaLevel());
  }
}
