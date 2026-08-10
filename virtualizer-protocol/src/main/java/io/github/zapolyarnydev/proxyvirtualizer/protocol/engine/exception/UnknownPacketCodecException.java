package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception;

import java.io.Serial;

public final class UnknownPacketCodecException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public UnknownPacketCodecException(String message) {
    super(message);
  }
}
