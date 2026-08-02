package io.github.zapolyarnydev.proxyvirtualizer.core.session;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record ConnectionId(@NotNull UUID id) {

  public ConnectionId {
    Objects.requireNonNull(id, "id");
  }
}
