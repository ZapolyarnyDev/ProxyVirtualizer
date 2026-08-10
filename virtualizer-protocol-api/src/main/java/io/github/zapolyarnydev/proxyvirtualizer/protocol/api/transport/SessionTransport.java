package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport;

import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.NotNull;

public interface SessionTransport {

  @NotNull
  TransportState state();

  void start(@NotNull TransportListener listener);

  @NotNull
  CompletionStage<Void> send(@NotNull OutboundFrame frame);

  @NotNull
  CompletionStage<Void> close(@NotNull TransportCloseReason reason);
}
