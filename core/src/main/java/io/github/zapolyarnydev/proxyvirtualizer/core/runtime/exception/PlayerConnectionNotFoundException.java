package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.io.Serial;

public final class PlayerConnectionNotFoundException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public PlayerConnectionNotFoundException(PlayerId playerId) {
    super("Player is not connected: " + playerId);
  }
}
