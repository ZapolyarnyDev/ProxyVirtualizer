package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolContext;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ServerboundPacket;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PacketActionMapper<P extends ServerboundPacket> {

  void map(
      @NotNull ProtocolContext context, @NotNull P packet, @NotNull SemanticActionSink actions);
}
