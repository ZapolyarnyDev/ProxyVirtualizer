package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.context;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ClientboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import org.jetbrains.annotations.NotNull;

public interface ProtocolContext {

  @NotNull
  ProtocolProfile profile();

  @NotNull
  ProtocolPhase phase();

  void send(@NotNull ClientboundPacket packet);
}
