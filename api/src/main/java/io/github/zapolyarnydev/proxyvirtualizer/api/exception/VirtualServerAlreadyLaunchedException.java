package io.github.zapolyarnydev.proxyvirtualizer.api.exception;

import java.io.Serial;

public class VirtualServerAlreadyLaunchedException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public VirtualServerAlreadyLaunchedException(String message) {
    super(message);
  }
}
