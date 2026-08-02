package io.github.zapolyarnydev.proxyvirtualizer.core.room.exception;

import java.io.Serial;

public final class SessionNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public SessionNotFoundException(String message) {
    super(message);
  }
}
