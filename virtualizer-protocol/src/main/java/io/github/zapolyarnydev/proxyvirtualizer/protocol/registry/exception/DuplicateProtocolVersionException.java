package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import java.io.Serial;

public final class DuplicateProtocolVersionException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicateProtocolVersionException(ProtocolProfile first, ProtocolProfile second) {
    super(
        "Protocol profiles claim the same protocol version: "
            + first.id()
            + " "
            + first.versions()
            + " and "
            + second.id()
            + " "
            + second.versions());
  }
}
