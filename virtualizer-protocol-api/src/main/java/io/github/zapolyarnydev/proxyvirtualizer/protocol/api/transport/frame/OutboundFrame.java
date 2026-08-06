package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.frame;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record OutboundFrame(@NotNull PacketId packetId, @NotNull ByteBuffer payload)
    implements TransportFrame {

  public OutboundFrame {
    Objects.requireNonNull(packetId, "packetId");
    Objects.requireNonNull(payload, "payload");
    payload = payload.asReadOnlyBuffer();
  }

  @Override
  public @NotNull ByteBuffer payload() {
    return payload.asReadOnlyBuffer();
  }
}
