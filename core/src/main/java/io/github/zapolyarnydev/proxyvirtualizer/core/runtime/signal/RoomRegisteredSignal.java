package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.RoomSnapshot;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record RoomRegisteredSignal(@NotNull RoomSnapshot room) implements RuntimeSignal {

  public RoomRegisteredSignal {
    Objects.requireNonNull(room, "room");
  }
}
