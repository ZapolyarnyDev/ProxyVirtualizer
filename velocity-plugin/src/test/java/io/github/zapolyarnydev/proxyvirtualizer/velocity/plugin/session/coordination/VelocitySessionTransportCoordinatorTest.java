package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.PlayerDisconnectedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.SessionClosedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.SessionOpenedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.PlayerConnectionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.SessionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.SessionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.SessionState;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.OutboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportCloseReason;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportListener;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportState;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityConnection;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityConnectionRegistry;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

final class VelocitySessionTransportCoordinatorTest {

  @Test
  void startsTransportWhenSessionOpens() {
    Fixture fixture = new Fixture();

    fixture.open();

    assertThat(fixture.transports).hasSize(1);
    assertThat(fixture.transport().startCount).isOne();
  }

  @Test
  void closesTransportWhenSessionCloses() {
    Fixture fixture = new Fixture();
    fixture.open();

    fixture.coordinator.onSignal(new SessionClosedSignal(fixture.closedSession()));

    assertThat(fixture.transport().closeReasons).containsExactly(TransportCloseReason.REQUESTED);
  }

  @Test
  void closesTransportWhenPlayerDisconnects() {
    Fixture fixture = new Fixture();
    fixture.open();

    fixture.coordinator.onSignal(
        new PlayerDisconnectedSignal(
            new PlayerConnectionSnapshot(
                fixture.connection.playerId(), fixture.connection.connectionId())));

    assertThat(fixture.transport().closeReasons)
        .containsExactly(TransportCloseReason.REMOTE_CLOSED);
  }

  @Test
  void closesCoreSessionWhenTransportFails() {
    Fixture fixture = new Fixture();
    fixture.open();

    fixture.transport().fail(new IllegalStateException("boom"));
    fixture.transport().fail(new IllegalStateException("again"));

    assertThat(fixture.closedPlayers).containsExactly(fixture.connection.playerId());
    assertThat(fixture.failures)
        .singleElement()
        .extracting(Throwable::getMessage)
        .isEqualTo("boom");
    assertThat(fixture.transport().closeReasons)
        .containsExactly(TransportCloseReason.TRANSPORT_FAILURE);
  }

  @Test
  void closesCoreSessionWhenTransportClosesUnexpectedly() {
    Fixture fixture = new Fixture();
    fixture.open();

    fixture.transport().remoteClose();

    assertThat(fixture.closedPlayers).containsExactly(fixture.connection.playerId());
    assertThat(fixture.failures).isEmpty();
  }

  @Test
  void closesCoreSessionWhenVelocityConnectionIsMissing() {
    Fixture fixture = new Fixture();
    fixture.connections.unregister(fixture.connection.player());

    fixture.open();

    assertThat(fixture.transports).isEmpty();
    assertThat(fixture.closedPlayers).containsExactly(fixture.connection.playerId());
    assertThat(fixture.failures)
        .singleElement()
        .extracting(Throwable::getMessage)
        .asString()
        .contains("Velocity connection is missing");
  }

  @Test
  void reportsAsynchronousTransportCloseFailure() {
    Fixture fixture = new Fixture(FailingCloseTransport::new);
    fixture.open();

    fixture.coordinator.onSignal(new SessionClosedSignal(fixture.closedSession()));

    assertThat(fixture.failures)
        .singleElement()
        .extracting(Throwable::getMessage)
        .isEqualTo("close failed");
  }

  @Test
  void reportsAsynchronousCoreSessionCloseFailure() {
    Fixture fixture = new Fixture();
    fixture.coreCloseResult =
        CompletableFuture.failedFuture(new IllegalStateException("core close failed"));
    fixture.open();

    fixture.transport().remoteClose();

    assertThat(fixture.failures)
        .singleElement()
        .extracting(Throwable::getMessage)
        .isEqualTo("core close failed");
  }

  @Test
  void reconcilesStartCallbackAndExceptionOnlyOnce() {
    Fixture fixture = new Fixture(FailingStartTransport::new);

    fixture.open();

    assertThat(fixture.closedPlayers).containsExactly(fixture.connection.playerId());
    assertThat(fixture.failures)
        .singleElement()
        .extracting(Throwable::getMessage)
        .isEqualTo("callback failure");
    assertThat(fixture.transport().closeReasons)
        .containsExactly(TransportCloseReason.TRANSPORT_FAILURE);
  }

  @Test
  void doesNotHoldCoordinatorLockWhileTransportStarts() throws Exception {
    BlockingStartTransport transport = new BlockingStartTransport();
    Fixture fixture = new Fixture(() -> transport);
    CompletableFuture<Void> opening = CompletableFuture.runAsync(fixture::open);
    assertThat(transport.started.await(2, TimeUnit.SECONDS)).isTrue();

    try {
      CompletableFuture<Void> closing =
          CompletableFuture.runAsync(
              () -> fixture.coordinator.onSignal(new SessionClosedSignal(fixture.closedSession())));

      closing.get(2, TimeUnit.SECONDS);
    } finally {
      transport.resume.countDown();
    }
    opening.get(2, TimeUnit.SECONDS);

    assertThat(transport.closeReasons()).containsExactly(TransportCloseReason.REQUESTED);
  }

  @Test
  void closeRejectsTransportCreatedByConcurrentOpen() throws Exception {
    BlockingTransportSupplier transportSupplier = new BlockingTransportSupplier();
    Fixture fixture = new Fixture(transportSupplier);
    CompletableFuture<Void> opening = CompletableFuture.runAsync(fixture::open);
    assertThat(transportSupplier.factoryEntered.await(2, TimeUnit.SECONDS)).isTrue();

    try {
      fixture.coordinator.close();
    } finally {
      transportSupplier.resumeFactory.countDown();
    }
    opening.get(2, TimeUnit.SECONDS);

    assertThat(fixture.transport().startCount).isZero();
    assertThat(fixture.transport().closeReasons).containsExactly(TransportCloseReason.REQUESTED);
  }

  @Test
  void ignoresRepeatedLifecycleSignals() {
    Fixture fixture = new Fixture();
    SessionSnapshot nextSession = fixture.nextSession();

    fixture.open();
    fixture.open();
    fixture.coordinator.onSignal(new SessionClosedSignal(fixture.closedSession()));
    fixture.coordinator.onSignal(new SessionClosedSignal(fixture.closedSession()));
    fixture.coordinator.onSignal(new SessionOpenedSignal(nextSession));
    fixture.coordinator.onSignal(new SessionClosedSignal(fixture.closedSession()));
    PlayerDisconnectedSignal disconnected =
        new PlayerDisconnectedSignal(
            new PlayerConnectionSnapshot(
                fixture.connection.playerId(), fixture.connection.connectionId()));
    fixture.coordinator.onSignal(disconnected);
    fixture.coordinator.onSignal(disconnected);

    assertThat(fixture.transports).hasSize(3);
    assertThat(fixture.transports.getFirst().closeReasons)
        .containsExactly(TransportCloseReason.REQUESTED);
    assertThat(fixture.transports.get(1).startCount).isZero();
    assertThat(fixture.transports.get(1).closeReasons)
        .containsExactly(TransportCloseReason.REQUESTED);
    assertThat(fixture.transports.getLast().startCount).isOne();
    assertThat(fixture.transports.getLast().closeReasons)
        .containsExactly(TransportCloseReason.REMOTE_CLOSED);
  }

  private static Player player(UUID uniqueId) {
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("getUniqueId")) return uniqueId;

              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static final class Fixture {

    private final VelocityConnectionRegistry connections = new VelocityConnectionRegistry();
    private final VelocityConnection connection =
        connections.register(player(UUID.randomUUID())).connection();
    private final SessionSnapshot session =
        new SessionSnapshot(
            SessionId.random(),
            connection.playerId(),
            connection.connectionId(),
            SessionState.ACTIVE,
            Instant.EPOCH);
    private final List<FakeTransport> transports = new ArrayList<>();
    private final List<PlayerId> closedPlayers = new ArrayList<>();
    private final List<Throwable> failures = new ArrayList<>();
    private CompletionStage<?> coreCloseResult = CompletableFuture.completedFuture(null);
    private final VelocitySessionTransportCoordinator coordinator;

    private Fixture() {
      this(FakeTransport::new);
    }

    private Fixture(Supplier<? extends FakeTransport> transportSupplier) {
      coordinator =
          new VelocitySessionTransportCoordinator(
              connections,
              ignored -> {
                FakeTransport transport = transportSupplier.get();
                transports.add(transport);
                return transport;
              },
              playerId -> {
                closedPlayers.add(playerId);
                return coreCloseResult;
              },
              (session, cause) -> failures.add(cause));
    }

    private void open() {
      coordinator.onSignal(new SessionOpenedSignal(session));
    }

    private SessionSnapshot closedSession() {
      return new SessionSnapshot(
          session.id(),
          session.playerId(),
          session.connectionId(),
          SessionState.CLOSED,
          session.createdAt());
    }

    private SessionSnapshot nextSession() {
      return new SessionSnapshot(
          SessionId.random(),
          session.playerId(),
          session.connectionId(),
          SessionState.ACTIVE,
          session.createdAt());
    }

    private FakeTransport transport() {
      return transports.getFirst();
    }
  }

  private static class FakeTransport implements SessionTransport {

    private final List<TransportCloseReason> closeReasons = new ArrayList<>();
    private TransportState state = TransportState.NEW;
    private TransportListener listener;
    private int startCount;

    @Override
    public @NotNull TransportState state() {
      return state;
    }

    @Override
    public void start(@NotNull TransportListener listener) {
      this.listener = listener;
      startCount++;
      state = TransportState.OPEN;
      listener.onOpened();
    }

    @Override
    public @NotNull CompletionStage<Void> send(@NotNull OutboundFrame frame) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletionStage<Void> close(@NotNull TransportCloseReason reason) {
      closeReasons.add(reason);
      state = TransportState.CLOSED;
      if (listener != null) listener.onClosed(reason);
      return CompletableFuture.completedFuture(null);
    }

    private void fail(Throwable cause) {
      state = TransportState.FAILED;
      listener.onFailure(cause);
    }

    private void remoteClose() {
      state = TransportState.CLOSED;
      listener.onClosed(TransportCloseReason.REMOTE_CLOSED);
    }

    final List<TransportCloseReason> closeReasons() {
      return closeReasons;
    }
  }

  private static final class FailingStartTransport extends FakeTransport {

    @Override
    public void start(@NotNull TransportListener listener) {
      super.start(listener);
      listener.onFailure(new IllegalStateException("callback failure"));
      throw new IllegalStateException("start failure");
    }
  }

  private static final class FailingCloseTransport extends FakeTransport {

    @Override
    public @NotNull CompletionStage<Void> close(@NotNull TransportCloseReason reason) {
      return CompletableFuture.failedFuture(new IllegalStateException("close failed"));
    }
  }

  private static final class BlockingStartTransport extends FakeTransport {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch resume = new CountDownLatch(1);

    @Override
    public void start(@NotNull TransportListener listener) {
      super.start(listener);
      started.countDown();
      try {
        if (!resume.await(5, TimeUnit.SECONDS))
          throw new IllegalStateException("Timed out waiting to resume transport start");
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Transport start interrupted", exception);
      }
    }
  }

  private static final class BlockingTransportSupplier implements Supplier<FakeTransport> {

    private final CountDownLatch factoryEntered = new CountDownLatch(1);
    private final CountDownLatch resumeFactory = new CountDownLatch(1);
    private final FakeTransport transport = new FakeTransport();

    @Override
    public FakeTransport get() {
      factoryEntered.countDown();
      try {
        if (!resumeFactory.await(5, TimeUnit.SECONDS))
          throw new IllegalStateException("Timed out waiting to resume transport factory");
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Transport factory interrupted", exception);
      }
      return transport;
    }
  }
}
