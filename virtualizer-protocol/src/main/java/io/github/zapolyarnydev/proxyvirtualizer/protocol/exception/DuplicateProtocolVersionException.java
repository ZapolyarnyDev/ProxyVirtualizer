package io.github.zapolyarnydev.proxyvirtualizer.protocol.exception;

import java.io.Serial;

public final class DuplicateProtocolVersionException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicateProtocolVersionException(String message) {
    super(message);
  }
}
