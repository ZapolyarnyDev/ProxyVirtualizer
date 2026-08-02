package io.github.zapolyarnydev.proxyvirtualizer.core.session;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record PlayerId(@NotNull UUID id) {

  public PlayerId {
    Objects.requireNonNull(id, "id");
  }
}
