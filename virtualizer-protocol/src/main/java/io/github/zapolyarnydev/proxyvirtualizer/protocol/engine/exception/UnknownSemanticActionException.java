package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception;

import java.io.Serial;

public final class UnknownSemanticActionException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public UnknownSemanticActionException(String message) {
    super(message);
  }
}
