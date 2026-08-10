package io.github.zapolyarnydev.proxyvirtualizer.velocity.netty.transport;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.OutboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportCloseReason;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportListener;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportState;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.netty.frame.VelocityFrameCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.NotNull;

public final class VelocitySessionTransport implements SessionTransport {

  private static final String HANDLER_NAME = "proxyvirtualizer-session-bridge";

  private final Channel channel;
  private final ChannelDuplexHandler handler = new BridgeHandler();
  private boolean started;
  private volatile TransportState state = TransportState.NEW;
  private volatile TransportListener listener;
  private volatile ChannelHandlerContext context;
  private volatile TransportCloseReason closeReason = TransportCloseReason.REMOTE_CLOSED;
  private final CompletableFuture<Void> closeCompletion = new CompletableFuture<>();

  private VelocitySessionTransport(Channel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
  }

  @NotNull
  public static VelocitySessionTransport create(@NotNull Player player) {
    Objects.requireNonNull(player, "player");
    return new VelocitySessionTransport(VelocityChannelAccess.channel(player));
  }

  static VelocitySessionTransport createForChannel(Channel channel) {
    return new VelocitySessionTransport(channel);
  }

  @Override
  public @NotNull TransportState state() {
    return state;
  }

  @Override
  public synchronized void start(@NotNull TransportListener listener) {
    Objects.requireNonNull(listener, "listener");
    if (started) throw new IllegalStateException("Transport has already been started");

    started = true;
    this.listener = listener;
    channel.eventLoop().execute(this::install);
  }

  @Override
  public @NotNull CompletionStage<Void> send(@NotNull OutboundFrame frame) {
    Objects.requireNonNull(frame, "frame");
    ChannelHandlerContext activeContext = context;
    if (state != TransportState.OPEN || activeContext == null)
      return CompletableFuture.failedFuture(new IllegalStateException("Transport is not open"));

    ByteBuf encoded = VelocityFrameCodec.encode(activeContext.alloc(), frame);
    try {
      return completionStage(channel.writeAndFlush(encoded));
    } catch (Throwable exception) {
      encoded.release();
      return CompletableFuture.failedFuture(exception);
    }
  }

  @Override
  public synchronized @NotNull CompletionStage<Void> close(@NotNull TransportCloseReason reason) {
    Objects.requireNonNull(reason, "reason");
    if (state.isTerminal()) return CompletableFuture.completedFuture(null);
    if (state == TransportState.CLOSING) return closeCompletion;

    closeReason = reason;
    state = TransportState.CLOSING;
    try {
      channel.eventLoop().execute(this::uninstall);
    } catch (Throwable exception) {
      failClose(exception);
    }
    return closeCompletion;
  }

  private void install() {
    if (state != TransportState.NEW) {
      if (state == TransportState.CLOSING) uninstall();
      return;
    }
    if (!channel.isActive()) {
      close(TransportCloseReason.REMOTE_CLOSED);
      return;
    }

    try {
      channel.pipeline().addBefore("minecraft-decoder", HANDLER_NAME, handler);
      context = channel.pipeline().context(HANDLER_NAME);
      state = TransportState.OPEN;
      notifyOpened();
    } catch (Throwable exception) {
      fail(exception);
    }
  }

  private void uninstall() {
    try {
      if (channel.pipeline().context(handler) != null) channel.pipeline().remove(handler);
      context = null;
      closeFromChannel();
      closeCompletion.complete(null);
    } catch (Throwable exception) {
      failClose(exception);
    }
  }

  private void handleInbound(ByteBuf input) {
    if (state != TransportState.OPEN) return;

    try {
      InboundFrame frame = VelocityFrameCodec.decode(input);
      listener.onInboundFrame(frame);
    } catch (Throwable exception) {
      fail(exception);
    }
  }

  private void closeFromChannel() {
    if (state.isTerminal()) return;

    state = TransportState.CLOSED;
    closeCompletion.complete(null);
    TransportListener activeListener = listener;
    if (activeListener == null) return;

    try {
      activeListener.onClosed(closeReason);
    } catch (Throwable ignored) {
    }
  }

  private void failClose(Throwable exception) {
    state = TransportState.FAILED;
    closeCompletion.completeExceptionally(exception);
    notifyFailure(exception);
  }

  private void fail(Throwable exception) {
    if (state.isTerminal()) return;

    state = TransportState.FAILED;
    closeReason = TransportCloseReason.TRANSPORT_FAILURE;
    closeCompletion.completeExceptionally(exception);
    notifyFailure(exception);
    channel.close();
  }

  private void notifyFailure(Throwable exception) {
    TransportListener activeListener = listener;
    if (activeListener != null) {
      try {
        activeListener.onFailure(exception);
      } catch (Throwable ignored) {
      }
    }
  }

  private void notifyOpened() {
    try {
      listener.onOpened();
    } catch (Throwable exception) {
      fail(exception);
    }
  }

  private static CompletionStage<Void> completionStage(ChannelFuture future) {
    CompletableFuture<Void> completion = new CompletableFuture<>();
    future.addListener(
        result -> {
          if (result.isSuccess()) completion.complete(null);
          else completion.completeExceptionally(result.cause());
        });
    return completion;
  }

  private final class BridgeHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
      if (message instanceof ByteBuf input) handleInbound(input);
      context.fireChannelRead(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
      closeFromChannel();
      context.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
      fail(cause);
      context.fireExceptionCaught(cause);
    }
  }
}
