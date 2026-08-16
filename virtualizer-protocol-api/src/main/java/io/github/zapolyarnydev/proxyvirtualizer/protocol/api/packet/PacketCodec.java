package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet;

import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public interface PacketCodec<P extends ProtocolPacket> {

  @NotNull
  Class<P> packetType();

  @NotNull
  P decode(@NotNull ByteBuffer input);

  // And this is where my attempt to document the code ends. I'll come back to it someday
  /** Returns a buffer whose remaining bytes are the complete packet payload. */
  @NotNull
  ByteBuffer encode(@NotNull P packet);
}
