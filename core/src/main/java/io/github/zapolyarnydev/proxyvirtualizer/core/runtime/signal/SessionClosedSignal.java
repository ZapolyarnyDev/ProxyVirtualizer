package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.SessionSnapshot;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record SessionClosedSignal(@NotNull SessionSnapshot session) implements RuntimeSignal {

  public SessionClosedSignal {
    Objects.requireNonNull(session, "session");
  }
}
