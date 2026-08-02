package io.github.zapolyarnydev.proxyvirtualizer.core.session;

import java.util.Objects;
import java.util.UUID;

public record SessionId(UUID value) {

  public SessionId {
    Objects.requireNonNull(value, "value");
  }

  public static SessionId random() {
    return new SessionId(UUID.randomUUID());
  }
}
