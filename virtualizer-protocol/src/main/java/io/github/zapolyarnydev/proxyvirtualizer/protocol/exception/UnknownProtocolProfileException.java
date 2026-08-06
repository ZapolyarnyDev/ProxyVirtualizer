package io.github.zapolyarnydev.proxyvirtualizer.protocol.exception;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolProfileId;
import java.io.Serial;

public final class UnknownProtocolProfileException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public UnknownProtocolProfileException(ProtocolProfileId profileId) {
    super("Unknown protocol profile: " + profileId);
  }
}
