package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.PlayerConnectionSnapshot;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record PlayerConnectedSignal(@NotNull PlayerConnectionSnapshot connection)
    implements RuntimeSignal {

  public PlayerConnectedSignal {
    Objects.requireNonNull(connection, "connection");
  }
}
