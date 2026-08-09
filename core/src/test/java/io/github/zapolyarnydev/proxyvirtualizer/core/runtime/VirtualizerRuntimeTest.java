package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.PlayerConnectionNotFoundException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.ProxyRoomAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.executor.RuntimeCommandExecutor;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.PlayerConnectedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.PlayerDisconnectedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RuntimeSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.SessionClosedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.SessionOpenedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.dispatch.RuntimeSignalDispatcher;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.PlayerConnectionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.SessionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.SessionState;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

final class VirtualizerRuntimeTest {

  private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void serializesCommandsBeforeMutatingRuntimeState() {
    ManualRuntimeCommandExecutor executor = new ManualRuntimeCommandExecutor();
    VirtualizerRuntime runtime = new VirtualizerRuntime(CLOCK, executor);
    PlayerId playerId = playerId();

    CompletionStage<PlayerConnectionSnapshot> connection =
        runtime.connect(playerId, connectionId());

    assertThat(connection).isNotCompleted();
    executor.runAll();
    assertThat(await(connection).playerId()).isEqualTo(playerId);
    assertThat(await(runtime.findSession(playerId), executor)).isEmpty();
  }

  @Test
  void opensPlayerSessionInExplicitlySelectedRoom() {
    ManualRuntimeCommandExecutor executor = new ManualRuntimeCommandExecutor();
    VirtualizerRuntime runtime = new VirtualizerRuntime(CLOCK, executor);
    RoomId firstRoomId = new RoomId(1);
    RoomId secondRoomId = new RoomId(2);
    PlayerId playerId = playerId();
    await(runtime.registerRoom(firstRoomId), executor);
    await(runtime.registerRoom(secondRoomId), executor);
    await(runtime.connect(playerId, connectionId()), executor);

    SessionSnapshot session = await(runtime.openSession(playerId, secondRoomId), executor);

    assertThat(session.state()).isEqualTo(SessionState.ACTIVE);
    assertThat(await(runtime.findSession(playerId), executor)).contains(session);
    assertThat(await(runtime.findRoom(firstRoomId), executor).orElseThrow().sessionCount())
        .isZero();
    assertThat(await(runtime.findRoom(secondRoomId), executor).orElseThrow().sessionCount())
        .isOne();
  }

  @Test
  void disconnectingPlayerClosesSessionInItsRoom() {
    ManualRuntimeCommandExecutor executor = new ManualRuntimeCommandExecutor();
    VirtualizerRuntime runtime = new VirtualizerRuntime(CLOCK, executor);
    RoomId roomId = new RoomId(1);
    PlayerId playerId = playerId();
    await(runtime.registerRoom(roomId), executor);
    PlayerConnectionSnapshot connection =
        await(runtime.connect(playerId, connectionId()), executor);
    await(runtime.openSession(playerId, roomId), executor);

    Optional<PlayerConnectionSnapshot> disconnected =
        await(runtime.disconnect(connection.connectionId()), executor);

    assertThat(disconnected).isPresent();
    assertThat(await(runtime.findConnection(playerId), executor)).isEmpty();
    assertThat(await(runtime.findSession(playerId), executor)).isEmpty();
    assertThat(await(runtime.findRoom(roomId), executor).orElseThrow().sessionCount()).isZero();
  }

  @Test
  void rejectsOpeningSessionForDisconnectedPlayer() {
    ManualRuntimeCommandExecutor executor = new ManualRuntimeCommandExecutor();
    VirtualizerRuntime runtime = new VirtualizerRuntime(CLOCK, executor);
    await(runtime.registerRoom(new RoomId(1)), executor);

    assertThatThrownBy(() -> await(runtime.openSession(playerId(), new RoomId(1)), executor))
        .hasCauseInstanceOf(PlayerConnectionNotFoundException.class);
  }

  @Test
  void rejectsDuplicateRoomRegistration() {
    ManualRuntimeCommandExecutor executor = new ManualRuntimeCommandExecutor();
    VirtualizerRuntime runtime = new VirtualizerRuntime(CLOCK, executor);
    await(runtime.registerRoom(new RoomId(1)), executor);

    assertThatThrownBy(() -> await(runtime.registerRoom(new RoomId(1)), executor))
        .hasCauseInstanceOf(ProxyRoomAlreadyExistsException.class);
  }

  @Test
  void ignoresStaleDisconnectAfterPlayerReconnects() {
    ManualRuntimeCommandExecutor executor = new ManualRuntimeCommandExecutor();
    VirtualizerRuntime runtime = new VirtualizerRuntime(CLOCK, executor);
    PlayerId playerId = playerId();
    ConnectionId firstConnectionId = connectionId();
    ConnectionId secondConnectionId = connectionId();
    await(runtime.connect(playerId, firstConnectionId), executor);
    await(runtime.disconnect(firstConnectionId), executor);
    await(runtime.connect(playerId, secondConnectionId), executor);

    Optional<PlayerConnectionSnapshot> disconnected =
        await(runtime.disconnect(firstConnectionId), executor);

    assertThat(disconnected).isEmpty();
    assertThat(await(runtime.findConnection(playerId), executor))
        .contains(new PlayerConnectionSnapshot(playerId, secondConnectionId));
  }

  @Test
  void publishesSignalsOutsideRuntimeCommandExecutor() {
    ManualRuntimeCommandExecutor runtimeExecutor = new ManualRuntimeCommandExecutor();
    ManualExecutor signalExecutor = new ManualExecutor();
    Queue<RuntimeSignal> signals = new ArrayDeque<>();
    VirtualizerRuntime runtime =
        new VirtualizerRuntime(
            CLOCK, runtimeExecutor, RuntimeSignalDispatcher.async(signalExecutor, signals::add));
    RoomId roomId = new RoomId(1);
    PlayerId playerId = playerId();
    ConnectionId connectionId = connectionId();

    await(runtime.registerRoom(roomId), runtimeExecutor);
    await(runtime.connect(playerId, connectionId), runtimeExecutor);
    await(runtime.openSession(playerId, roomId), runtimeExecutor);
    await(runtime.disconnect(connectionId), runtimeExecutor);

    assertThat(signals).isEmpty();
    signalExecutor.runAll();
    assertThat(signals)
        .hasExactlyElementsOfTypes(
            io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RoomRegisteredSignal.class,
            PlayerConnectedSignal.class,
            SessionOpenedSignal.class,
            SessionClosedSignal.class,
            PlayerDisconnectedSignal.class);
  }

  private static <T> T await(CompletionStage<T> stage, ManualRuntimeCommandExecutor executor) {
    executor.runAll();
    return await(stage);
  }

  private static <T> T await(CompletionStage<T> stage) {
    return stage.toCompletableFuture().join();
  }

  private static PlayerId playerId() {
    return new PlayerId(UUID.randomUUID());
  }

  private static ConnectionId connectionId() {
    return new ConnectionId(UUID.randomUUID());
  }

  private static final class ManualRuntimeCommandExecutor implements RuntimeCommandExecutor {

    private final Queue<Runnable> commands = new ArrayDeque<>();

    @Override
    public <T> CompletionStage<T> submit(Callable<? extends T> command) {
      CompletableFuture<T> result = new CompletableFuture<>();
      commands.add(
          () -> {
            try {
              result.complete(command.call());
            } catch (Throwable throwable) {
              result.completeExceptionally(throwable);
            }
          });
      return result;
    }

    @Override
    public void close() {}

    void runAll() {
      while (!commands.isEmpty()) commands.remove().run();
    }
  }

  private static final class ManualExecutor implements Executor {

    private final Queue<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void execute(Runnable command) {
      tasks.add(command);
    }

    void runAll() {
      while (!tasks.isEmpty()) tasks.remove().run();
    }
  }
}
