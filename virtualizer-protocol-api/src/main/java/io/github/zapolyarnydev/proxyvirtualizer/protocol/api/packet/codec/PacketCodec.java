package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.codec;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.PacketDirection;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ProtocolPacket;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public interface PacketCodec<P extends ProtocolPacket> {

  @NotNull
  PacketId packetId();

  @NotNull
  PacketDirection direction();

  @NotNull
  Class<P> packetType();

  @NotNull
  P decode(@NotNull ByteBuffer input);

  void encode(@NotNull P packet, @NotNull ByteBuffer output);
}
