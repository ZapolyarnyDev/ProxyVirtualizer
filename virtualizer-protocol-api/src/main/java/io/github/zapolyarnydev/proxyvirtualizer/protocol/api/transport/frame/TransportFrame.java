package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.frame;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public sealed interface TransportFrame permits InboundFrame, OutboundFrame {

  @NotNull
  PacketId packetId();

  @NotNull
  ByteBuffer payload();
}
