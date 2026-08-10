package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import java.io.Serial;

public final class UnknownProtocolProfileException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public UnknownProtocolProfileException(ProtocolProfileId profileId) {
    super("Unknown protocol profile: " + profileId);
  }
}
