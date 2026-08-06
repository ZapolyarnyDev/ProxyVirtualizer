package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.SessionSnapshot;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record SessionOpenedSignal(@NotNull SessionSnapshot session) implements RuntimeSignal {

  public SessionOpenedSignal {
    Objects.requireNonNull(session, "session");
  }
}
