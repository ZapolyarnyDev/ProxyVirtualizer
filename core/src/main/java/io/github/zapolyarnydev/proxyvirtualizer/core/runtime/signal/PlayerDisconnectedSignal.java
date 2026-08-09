package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.PlayerConnectionSnapshot;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record PlayerDisconnectedSignal(@NotNull PlayerConnectionSnapshot connection)
    implements RuntimeSignal {

  public PlayerDisconnectedSignal {
    Objects.requireNonNull(connection, "connection");
  }
}
