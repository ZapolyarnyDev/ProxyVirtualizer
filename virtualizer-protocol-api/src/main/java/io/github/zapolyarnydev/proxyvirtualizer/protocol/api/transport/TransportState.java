package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport;

public enum TransportState {
  NEW,
  OPEN,
  CLOSING,
  CLOSED,
  FAILED;

  public boolean isOpen() {
    return this == OPEN;
  }

  public boolean isTerminal() {
    return this == CLOSED || this == FAILED;
  }
}
