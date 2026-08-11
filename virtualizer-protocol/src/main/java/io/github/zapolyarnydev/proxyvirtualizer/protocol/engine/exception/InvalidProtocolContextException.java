package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception;

import java.io.Serial;

public final class InvalidProtocolContextException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public InvalidProtocolContextException(String message) {
    super(message);
  }
}
