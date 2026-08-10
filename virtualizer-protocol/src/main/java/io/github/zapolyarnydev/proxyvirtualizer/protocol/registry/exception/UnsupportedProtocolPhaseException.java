package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import java.io.Serial;

public final class UnsupportedProtocolPhaseException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public UnsupportedProtocolPhaseException(ProtocolProfile profile, ProtocolPhase phase) {
    super("Protocol profile " + profile.id() + " does not support phase: " + phase);
  }
}
