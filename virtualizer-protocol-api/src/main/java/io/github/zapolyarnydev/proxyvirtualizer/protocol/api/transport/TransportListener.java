package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport;

import org.jetbrains.annotations.NotNull;

public interface TransportListener {

  void onOpened();

  void onInboundFrame(@NotNull InboundFrame frame);

  void onClosed(@NotNull TransportCloseReason reason);

  void onFailure(@NotNull Throwable cause);
}
