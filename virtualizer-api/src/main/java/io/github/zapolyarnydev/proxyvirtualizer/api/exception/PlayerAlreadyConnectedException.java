package io.github.zapolyarnydev.proxyvirtualizer.api.exception;

import java.io.Serial;

public class PlayerAlreadyConnectedException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public PlayerAlreadyConnectedException(String message) {
    super(message);
  }
}
