package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet;

import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public interface PacketCodec<P extends ProtocolPacket> {

  @NotNull
  Class<P> packetType();

  @NotNull
  P decode(@NotNull ByteBuffer input);

  /** Returns a buffer whose remaining bytes are the complete packet payload. */
  @NotNull
  ByteBuffer encode(@NotNull P packet);
}
