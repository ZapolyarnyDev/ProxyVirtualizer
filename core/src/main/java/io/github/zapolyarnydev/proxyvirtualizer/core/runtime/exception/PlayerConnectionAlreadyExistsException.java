package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.io.Serial;

public final class PlayerConnectionAlreadyExistsException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public PlayerConnectionAlreadyExistsException(PlayerId playerId, ConnectionId connectionId) {
    super("Player is already connected: " + playerId + " with connection: " + connectionId);
  }
}
