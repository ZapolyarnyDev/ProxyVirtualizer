package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.frame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.id.PacketId;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import org.junit.jupiter.api.Test;

final class TransportFrameTest {

  @Test
  void exposesReadOnlyPayload() {
    InboundFrame frame = new InboundFrame(new PacketId(42), ByteBuffer.wrap(new byte[] {1, 2}));

    assertThat(frame.payload().isReadOnly()).isTrue();
    assertThat(frame.payload().get()).isEqualTo((byte) 1);
    assertThatThrownBy(() -> frame.payload().put((byte) 3))
        .isInstanceOf(ReadOnlyBufferException.class);
  }

  @Test
  void exposesIndependentBufferViews() {
    OutboundFrame frame = new OutboundFrame(new PacketId(42), ByteBuffer.wrap(new byte[] {1, 2}));
    ByteBuffer first = frame.payload();
    first.get();

    assertThat(frame.payload().get()).isEqualTo((byte) 1);
  }
}
