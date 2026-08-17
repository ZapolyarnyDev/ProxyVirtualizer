package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolPacket;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

final class KnownPacksCodec<P extends ProtocolPacket> implements PacketCodec<P> {
  private final Class<P> type;
  private final Function<List<KnownPack>, P> factory;
  private final Function<P, List<KnownPack>> packs;

  KnownPacksCodec(
      Class<P> type, Function<List<KnownPack>, P> factory, Function<P, List<KnownPack>> packs) {
    this.type = type;
    this.factory = factory;
    this.packs = packs;
  }

  @Override
  public @NotNull Class<P> packetType() {
    return type;
  }

  @Override
  public @NotNull P decode(@NotNull ByteBuffer input) {
    int size = MinecraftWire.readVarInt(input);
    List<KnownPack> result = new ArrayList<>(size);
    for (int index = 0; index < size; index++)
      result.add(
          new KnownPack(
              MinecraftWire.readString(input),
              MinecraftWire.readString(input),
              MinecraftWire.readString(input)));
    return factory.apply(List.copyOf(result));
  }

  @Override
  public @NotNull ByteBuffer encode(@NotNull P packet) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    List<KnownPack> values = packs.apply(packet);
    MinecraftWire.writeVarInt(output, values.size());
    values.forEach(
        value -> {
          MinecraftWire.writeString(output, value.namespace());
          MinecraftWire.writeString(output, value.id());
          MinecraftWire.writeString(output, value.version());
        });
    return MinecraftWire.buffer(output);
  }
}
