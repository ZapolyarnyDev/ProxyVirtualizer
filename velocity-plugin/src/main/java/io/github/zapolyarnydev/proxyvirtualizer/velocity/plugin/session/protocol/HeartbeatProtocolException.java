package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import java.io.Serial;

final class HeartbeatProtocolException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  HeartbeatProtocolException(String message) {
    super(message);
  }
}
