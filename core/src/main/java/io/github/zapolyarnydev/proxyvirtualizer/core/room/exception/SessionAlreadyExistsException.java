package io.github.zapolyarnydev.proxyvirtualizer.core.room.exception;

import java.io.Serial;

public final class SessionAlreadyExistsException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public SessionAlreadyExistsException(String message) {
    super(message);
  }
}
