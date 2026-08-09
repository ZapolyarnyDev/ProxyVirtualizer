package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.ProxyRoom;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record RoomSnapshot(@NotNull RoomId id, int sessionCount, boolean full) {

  public RoomSnapshot {
    Objects.requireNonNull(id, "id");
    if (sessionCount < 0) throw new IllegalArgumentException("Session count cannot be negative");
  }

  @NotNull
  public static RoomSnapshot from(ProxyRoom room) {
    return new RoomSnapshot(room.id(), room.sessions().size(), room.isFull());
  }
}
