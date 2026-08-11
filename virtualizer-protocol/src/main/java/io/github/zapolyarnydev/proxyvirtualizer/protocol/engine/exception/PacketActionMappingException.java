package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception;

import java.io.Serial;

public final class PacketActionMappingException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public PacketActionMappingException(String message, Throwable cause) {
    super(message, cause);
  }
}
