package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record VelocityConnectionRegistration(
    @NotNull VelocityConnection connection, boolean created) {

  public VelocityConnectionRegistration {
    Objects.requireNonNull(connection, "connection");
  }
}
