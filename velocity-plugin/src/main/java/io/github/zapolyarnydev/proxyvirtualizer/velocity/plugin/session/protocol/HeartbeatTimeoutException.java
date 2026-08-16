package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

public final class HeartbeatTimeoutException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  public HeartbeatTimeoutException(String message) {
    super(message);
  }
}
