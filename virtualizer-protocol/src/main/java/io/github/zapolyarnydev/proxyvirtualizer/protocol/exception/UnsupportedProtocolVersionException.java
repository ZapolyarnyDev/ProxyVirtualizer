package io.github.zapolyarnydev.proxyvirtualizer.protocol.exception;

import java.io.Serial;

public final class UnsupportedProtocolVersionException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public UnsupportedProtocolVersionException(String message) {
    super(message);
  }
}
