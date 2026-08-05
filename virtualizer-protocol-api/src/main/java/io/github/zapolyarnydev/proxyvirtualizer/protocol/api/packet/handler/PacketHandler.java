package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.handler;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.context.ProtocolContext;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PacketHandler<P extends ServerboundPacket> {

  void handle(@NotNull ProtocolContext context, @NotNull P packet);
}
