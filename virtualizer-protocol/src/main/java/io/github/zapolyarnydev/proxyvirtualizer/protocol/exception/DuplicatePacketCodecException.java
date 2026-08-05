package io.github.zapolyarnydev.proxyvirtualizer.protocol.exception;

import java.io.Serial;

public final class DuplicatePacketCodecException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public DuplicatePacketCodecException(String message) {
    super(message);
  }
}
