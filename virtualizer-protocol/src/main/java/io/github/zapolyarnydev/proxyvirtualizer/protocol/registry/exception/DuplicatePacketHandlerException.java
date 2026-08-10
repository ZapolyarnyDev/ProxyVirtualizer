package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception;

import java.io.Serial;

public final class DuplicatePacketHandlerException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicatePacketHandlerException(String message) {
    super(message);
  }
}
