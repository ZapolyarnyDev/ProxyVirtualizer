package io.github.zapolyarnydev.proxyvirtualizer.core.room.exception;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import java.io.Serial;

public final class ProxyRoomFullException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 1L;

  public ProxyRoomFullException(RoomId roomId) {
    super("Proxy room is full: " + roomId);
  }
}
