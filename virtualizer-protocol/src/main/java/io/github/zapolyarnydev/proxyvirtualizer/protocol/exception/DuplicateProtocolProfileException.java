package io.github.zapolyarnydev.proxyvirtualizer.protocol.exception;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import java.io.Serial;

public final class DuplicateProtocolProfileException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicateProtocolProfileException(ProtocolProfileId profileId) {
    super("Protocol profile is already registered: " + profileId);
  }
}
