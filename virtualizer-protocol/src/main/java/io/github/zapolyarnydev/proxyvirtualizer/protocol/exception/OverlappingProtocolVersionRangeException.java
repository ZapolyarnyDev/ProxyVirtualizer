package io.github.zapolyarnydev.proxyvirtualizer.protocol.exception;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import java.io.Serial;

public final class OverlappingProtocolVersionRangeException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public OverlappingProtocolVersionRangeException(ProtocolProfile first, ProtocolProfile second) {
    super(
        "Protocol profile version ranges overlap: "
            + first.id()
            + " "
            + first.versions()
            + " and "
            + second.id()
            + " "
            + second.versions());
  }
}
