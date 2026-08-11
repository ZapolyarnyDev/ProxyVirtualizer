package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportCloseReason;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportListener;
import java.util.Objects;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

final class VelocitySessionTransportListener implements TransportListener {

  private final Runnable openAction;
  private final Consumer<InboundFrame> inboundAction;
  private final Consumer<Throwable> protocolFailureAction;
  private final Consumer<TransportCloseReason> closeAction;
  private final Consumer<Throwable> transportFailureAction;

  VelocitySessionTransportListener(
      Runnable openAction,
      Consumer<InboundFrame> inboundAction,
      Consumer<Throwable> protocolFailureAction,
      Consumer<TransportCloseReason> closeAction,
      Consumer<Throwable> transportFailureAction) {
    this.openAction = Objects.requireNonNull(openAction, "openAction");
    this.inboundAction = Objects.requireNonNull(inboundAction, "inboundAction");
    this.protocolFailureAction =
        Objects.requireNonNull(protocolFailureAction, "protocolFailureAction");
    this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
    this.transportFailureAction =
        Objects.requireNonNull(transportFailureAction, "transportFailureAction");
  }

  @Override
  public void onOpened() {
    runProtocol(openAction);
  }

  @Override
  public void onInboundFrame(@NotNull InboundFrame frame) {
    Objects.requireNonNull(frame, "frame");
    runProtocol(() -> inboundAction.accept(frame));
  }

  @Override
  public void onClosed(@NotNull TransportCloseReason reason) {
    closeAction.accept(reason);
  }

  @Override
  public void onFailure(@NotNull Throwable cause) {
    transportFailureAction.accept(cause);
  }

  private void runProtocol(Runnable action) {
    try {
      action.run();
    } catch (RuntimeException cause) {
      protocolFailureAction.accept(cause);
    }
  }
}
