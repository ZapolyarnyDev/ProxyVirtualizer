package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.OutboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportCloseReason;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportListener;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportState;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft.Minecraft26_2Protocol;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.netty.frame.VelocityFrameCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

final class VelocitySessionProtocolRawBytesTest {

  private static final long KEEP_ALIVE_ID = -7_612_481_992L;

  @Test
  void carriesKeepAliveBetweenRawClientAndServerBytes() {
    ProtocolRegistry registry = new ProtocolRegistry();
    Minecraft26_2Protocol.install(registry);
    ProtocolVersion version = Minecraft26_2Protocol.VERSION;
    ProtocolProfile profile = registry.profiles().require(version, ProtocolPhase.PLAY);
    RecordingTransport transport = new RecordingTransport();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    try {
      VelocitySessionProtocol protocol =
          new VelocitySessionProtocolFactory(
                  registry, () -> KEEP_ALIVE_ID, scheduler, Duration.ofMinutes(1))
              .create(
                  version,
                  profile,
                  ProtocolPhase.PLAY,
                  transport,
                  cause -> {
                    throw new AssertionError("Unexpected asynchronous protocol failure", cause);
                  });

      protocol.onOpened();

      ByteBuf outbound =
          VelocityFrameCodec.encode(UnpooledByteBufAllocator.DEFAULT, transport.frame);
      assertThat(outbound.readUnsignedByte())
          .isEqualTo((short) Minecraft26_2Protocol.CLIENTBOUND_KEEP_ALIVE_ID.value());
      assertThat(outbound.readLong()).isEqualTo(KEEP_ALIVE_ID);
      outbound.release();

      ByteBuf clientBytes = Unpooled.buffer();
      clientBytes.writeByte(Minecraft26_2Protocol.SERVERBOUND_KEEP_ALIVE_ID.value());
      clientBytes.writeLong(KEEP_ALIVE_ID);
      protocol.onInboundFrame(VelocityFrameCodec.decode(clientBytes));
      clientBytes.release();

      assertThat(protocol.isHeartbeatAcknowledged()).isTrue();
    } finally {
      scheduler.shutdownNow();
    }
  }

  private static final class RecordingTransport implements SessionTransport {

    private OutboundFrame frame;

    @Override
    public @NotNull TransportState state() {
      return TransportState.OPEN;
    }

    @Override
    public void start(@NotNull TransportListener listener) {}

    @Override
    public @NotNull CompletionStage<Void> send(@NotNull OutboundFrame frame) {
      this.frame = frame;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletionStage<Void> close(@NotNull TransportCloseReason reason) {
      return CompletableFuture.completedFuture(null);
    }
  }
}
