package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PacketHandler<P extends ServerboundPacket> {

  void handle(@NotNull ProtocolContext context, @NotNull P packet);
}
