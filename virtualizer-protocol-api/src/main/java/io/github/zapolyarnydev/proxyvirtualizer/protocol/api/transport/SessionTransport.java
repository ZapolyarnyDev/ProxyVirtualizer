package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.frame.OutboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.lifecycle.TransportCloseReason;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.lifecycle.TransportListener;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.lifecycle.TransportState;
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
