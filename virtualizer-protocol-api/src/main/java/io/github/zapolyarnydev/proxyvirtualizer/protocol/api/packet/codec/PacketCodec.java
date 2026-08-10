package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.codec;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ProtocolPacket;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public interface PacketCodec<P extends ProtocolPacket> {

  @NotNull
  Class<P> packetType();

  @NotNull
  P decode(@NotNull ByteBuffer input);

  void encode(@NotNull P packet, @NotNull ByteBuffer output);
}
