package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.context;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ClientboundPacket;
import org.jetbrains.annotations.NotNull;

public interface ProtocolContext {

  @NotNull
  ProtocolProfile profile();

  void send(@NotNull ClientboundPacket packet);
}
