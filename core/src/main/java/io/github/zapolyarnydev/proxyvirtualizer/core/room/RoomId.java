package io.github.zapolyarnydev.proxyvirtualizer.core.room;

public record RoomId(long value) {

  public RoomId {
    if (value < 0) throw new IllegalArgumentException("Room id cannot be negative");
  }
}
