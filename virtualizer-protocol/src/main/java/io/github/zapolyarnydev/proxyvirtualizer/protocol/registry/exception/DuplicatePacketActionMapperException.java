package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception;

import java.io.Serial;

public final class DuplicatePacketActionMapperException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicatePacketActionMapperException(String message) {
    super(message);
  }
}
