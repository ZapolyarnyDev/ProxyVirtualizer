package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketId;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record InboundFrame(@NotNull PacketId packetId, @NotNull ByteBuffer payload)
    implements TransportFrame {

  public InboundFrame {
    Objects.requireNonNull(packetId, "packetId");
    Objects.requireNonNull(payload, "payload");
    payload = payload.asReadOnlyBuffer();
  }

  @Override
  public @NotNull ByteBuffer payload() {
    return payload.asReadOnlyBuffer();
  }
}
