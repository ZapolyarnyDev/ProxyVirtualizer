package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketCodec;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolPacket;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record RegisteredPacketCodec<P extends ProtocolPacket>(
    @NotNull PacketId packetId, @NotNull PacketCodec<P> codec) {

  public RegisteredPacketCodec {
    Objects.requireNonNull(packetId, "packetId");
    Objects.requireNonNull(codec, "codec");
  }
}
