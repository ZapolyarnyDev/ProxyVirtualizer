package io.github.zapolyarnydev.proxyvirtualizer.core.room;

import java.util.OptionalInt;
import org.jetbrains.annotations.NotNull;

record SessionLimit(int max) {

  private static final int UNLIMITED = -1;
  private static final SessionLimit UNLIMITED_INSTANCE = new SessionLimit(UNLIMITED);

  SessionLimit {
    if (max < UNLIMITED) {
      throw new IllegalArgumentException("Limit must be -1 (unlimited) or a non-negative value");
    }
  }

  static SessionLimit unlimited() {
    return UNLIMITED_INSTANCE;
  }

  static SessionLimit limited(int limit) {
    return new SessionLimit(limit);
  }

  boolean isUnlimited() {
    return max == UNLIMITED;
  }

  boolean isReached(int currentSessions) {
    return !isUnlimited() && currentSessions >= max;
  }

  OptionalInt value() {
    return isUnlimited() ? OptionalInt.empty() : OptionalInt.of(max);
  }

  @Override
  public @NotNull String toString() {
    return isUnlimited() ? "unlimited" : Integer.toString(max);
  }
}
