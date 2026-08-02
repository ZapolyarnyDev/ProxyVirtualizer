package io.github.zapolyarnydev.proxyvirtualizer.core.session.exception;

import java.io.Serial;

public final class SessionStateException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public SessionStateException(String message) {
    super(message);
  }
}
