package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport;

public enum TransportCloseReason {
  REQUESTED,
  REMOTE_CLOSED,
  PROTOCOL_ERROR,
  TRANSPORT_FAILURE
}
