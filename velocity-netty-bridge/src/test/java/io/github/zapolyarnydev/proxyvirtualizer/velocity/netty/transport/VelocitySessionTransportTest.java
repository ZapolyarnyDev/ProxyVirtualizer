package io.github.zapolyarnydev.proxyvirtualizer.velocity.netty.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.PacketId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.OutboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportCloseReason;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportListener;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

final class VelocitySessionTransportTest {

  @Test
  void bridgesFramesAndChannelLifecycle() {
    EmbeddedChannel channel = new EmbeddedChannel();
    channel.pipeline().addLast("minecraft-decoder", new ChannelInboundHandlerAdapter());
    RecordingOutboundHandler compressionEncoder = new RecordingOutboundHandler();
    channel.pipeline().addLast("compression-encoder", compressionEncoder);
    RecordingListener listener = new RecordingListener();
    VelocitySessionTransport transport = VelocitySessionTransport.createForChannel(channel);

    transport.start(listener);
    channel.runPendingTasks();

    assertThat(transport.state()).isEqualTo(TransportState.OPEN);
    assertThat(listener.opened).isTrue();

    ByteBuf inbound = Unpooled.wrappedBuffer(new byte[] {42, 1, 2});
    assertThat(channel.writeInbound(inbound)).isTrue();
    assertThat(listener.inboundFrame.packetId()).isEqualTo(new PacketId(42));
    assertThat(listener.inboundFrame.payload()).isEqualTo(ByteBuffer.wrap(new byte[] {1, 2}));
    ByteBuf forwardedInbound = channel.readInbound();
    forwardedInbound.release();

    transport
        .send(new OutboundFrame(new PacketId(42), ByteBuffer.wrap(new byte[] {3, 4})))
        .toCompletableFuture()
        .join();
    ByteBuf outbound = channel.readOutbound();
    assertThat(compressionEncoder.invoked).isTrue();
    assertThat(outbound.readByte()).isEqualTo((byte) 42);
    assertThat(outbound.readByte()).isEqualTo((byte) 3);
    assertThat(outbound.readByte()).isEqualTo((byte) 4);
    outbound.release();

    var closing = transport.close(TransportCloseReason.REQUESTED);
    channel.runPendingTasks();
    closing.toCompletableFuture().join();

    assertThat(listener.closeReason).isEqualTo(TransportCloseReason.REQUESTED);
    assertThat(transport.state()).isEqualTo(TransportState.CLOSED);
    assertThat(channel.isActive()).isTrue();
    assertThat(channel.pipeline().context("proxyvirtualizer-session-bridge")).isNull();
  }

  @Test
  void closeBeforeInstallDoesNotClosePlayerChannel() {
    EmbeddedChannel channel = new EmbeddedChannel();
    channel.pipeline().addLast("minecraft-decoder", new ChannelInboundHandlerAdapter());
    RecordingListener listener = new RecordingListener();
    VelocitySessionTransport transport = VelocitySessionTransport.createForChannel(channel);

    transport.start(listener);
    var closing = transport.close(TransportCloseReason.REQUESTED);
    channel.runPendingTasks();
    closing.toCompletableFuture().join();

    assertThat(channel.isActive()).isTrue();
    assertThat(transport.state()).isEqualTo(TransportState.CLOSED);
    assertThat(listener.opened).isFalse();
    assertThat(listener.closeReason).isEqualTo(TransportCloseReason.REQUESTED);
  }

  @Test
  void keepsFailedStateAndClosesChannelWhenListenerFails() {
    EmbeddedChannel channel = new EmbeddedChannel();
    channel.pipeline().addLast("minecraft-decoder", new ChannelInboundHandlerAdapter());
    RecordingListener listener = new RecordingListener();
    listener.openFailure = new IllegalStateException("boom");
    VelocitySessionTransport transport = VelocitySessionTransport.createForChannel(channel);

    transport.start(listener);
    channel.runPendingTasks();

    assertThat(transport.state()).isEqualTo(TransportState.FAILED);
    assertThat(channel.isActive()).isFalse();
    assertThat(listener.failure).isSameAs(listener.openFailure);
    assertThat(listener.closeReason).isNull();
  }

  @Test
  void failsPendingCloseWhenTransportFails() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel();
    channel.pipeline().addLast("minecraft-decoder", new ChannelInboundHandlerAdapter());
    RecordingListener listener = new RecordingListener();
    VelocitySessionTransport transport = VelocitySessionTransport.createForChannel(channel);
    transport.start(listener);
    channel.runPendingTasks();
    RuntimeException failure = new IllegalStateException("transport failed while closing");

    var closing = transport.close(TransportCloseReason.REQUESTED);
    ChannelHandlerContext bridge = channel.pipeline().context("proxyvirtualizer-session-bridge");
    ((ChannelDuplexHandler) bridge.handler()).exceptionCaught(bridge, failure);

    assertThatThrownBy(() -> closing.toCompletableFuture().join()).hasCause(failure);
    assertThat(listener.failure).isSameAs(failure);
    assertThat(transport.state()).isEqualTo(TransportState.FAILED);
  }

  private static final class RecordingListener implements TransportListener {

    private boolean opened;
    private InboundFrame inboundFrame;
    private TransportCloseReason closeReason;
    private RuntimeException openFailure;
    private Throwable failure;

    @Override
    public void onOpened() {
      if (openFailure != null) throw openFailure;
      opened = true;
    }

    @Override
    public void onInboundFrame(InboundFrame frame) {
      inboundFrame = frame;
    }

    @Override
    public void onClosed(TransportCloseReason reason) {
      closeReason = reason;
    }

    @Override
    public void onFailure(Throwable cause) {
      failure = cause;
    }
  }

  private static final class RecordingOutboundHandler extends ChannelOutboundHandlerAdapter {

    private boolean invoked;

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise)
        throws Exception {
      invoked = true;
      context.write(message, promise);
    }
  }
}
