package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.lifecycle;

public enum TransportCloseReason {
  REQUESTED,
  REMOTE_CLOSED,
  PROTOCOL_ERROR,
  TRANSPORT_FAILURE
}
