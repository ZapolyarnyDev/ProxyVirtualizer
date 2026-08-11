package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import org.jetbrains.annotations.NotNull;

public interface ProtocolContext {

  @NotNull
  ProtocolVersion version();

  @NotNull
  ProtocolProfile profile();

  @NotNull
  ProtocolPhase phase();

  void send(@NotNull ClientboundPacket packet);
}
