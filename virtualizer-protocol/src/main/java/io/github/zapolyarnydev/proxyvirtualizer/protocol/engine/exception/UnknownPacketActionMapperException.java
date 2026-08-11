package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception;

import java.io.Serial;

public final class UnknownPacketActionMapperException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public UnknownPacketActionMapperException(String message) {
    super(message);
  }
}
