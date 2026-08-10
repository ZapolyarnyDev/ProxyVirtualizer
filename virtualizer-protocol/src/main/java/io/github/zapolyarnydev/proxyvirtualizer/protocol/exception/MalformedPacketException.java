package io.github.zapolyarnydev.proxyvirtualizer.protocol.exception;

import java.io.Serial;

public final class MalformedPacketException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public MalformedPacketException(String message) {
    super(message);
  }

  public MalformedPacketException(String message, Throwable cause) {
    super(message, cause);
  }
}
