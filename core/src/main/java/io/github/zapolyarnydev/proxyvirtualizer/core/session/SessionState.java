package io.github.zapolyarnydev.proxyvirtualizer.core.session;

public enum SessionState {
  INITIALIZING,
  READY,
  CONNECTING,
  ACTIVE,
  SWITCHING,
  CLOSING,
  CLOSED
}
