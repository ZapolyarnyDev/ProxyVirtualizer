package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import java.io.Serial;

public final class ProxyRoomNotFoundException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  public ProxyRoomNotFoundException(RoomId roomId) {
    super("No proxy room is registered with id: " + roomId);
  }
}
