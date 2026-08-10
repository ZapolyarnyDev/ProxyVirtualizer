package io.github.zapolyarnydev.proxyvirtualizer.plugin.session.coordination;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.frame.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.lifecycle.TransportCloseReason;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.lifecycle.TransportListener;
import java.util.Objects;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

final class VelocitySessionTransportListener implements TransportListener {

  private final Consumer<TransportCloseReason> closeAction;
  private final Consumer<Throwable> failureAction;

  VelocitySessionTransportListener(
      Consumer<TransportCloseReason> closeAction, Consumer<Throwable> failureAction) {
    this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
    this.failureAction = Objects.requireNonNull(failureAction, "failureAction");
  }

  @Override
  public void onOpened() {}

  @Override
  public void onInboundFrame(@NotNull InboundFrame frame) {}

  @Override
  public void onClosed(@NotNull TransportCloseReason reason) {
    closeAction.accept(reason);
  }

  @Override
  public void onFailure(@NotNull Throwable cause) {
    failureAction.accept(cause);
  }
}
