package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import java.io.Serial;

public final class ProxyRoomAlreadyExistsException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public ProxyRoomAlreadyExistsException(RoomId roomId) {
    super("A proxy room is already registered with id: " + roomId);
  }
}
