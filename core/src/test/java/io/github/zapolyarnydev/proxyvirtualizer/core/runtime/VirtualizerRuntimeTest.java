package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.PlayerConnectionNotFoundException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.ProxyRoomAlreadyExistsException;
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
import org.junit.jupiter.api.Test;

final class VirtualizerRuntimeTest {

  private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void serializesCommandsBeforeMutatingRuntimeState() {
    ManualRuntimeCommandExecutor executor = new ManualRuntimeCommandExecutor();
    VirtualizerRuntime runtime = new VirtualizerRuntime(CLOCK, executor);
    PlayerId playerId = playerId();

    CompletionStage<PlayerConnectionSnapshot> connection = runtime.connect(playerId);

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
    await(runtime.connect(playerId), executor);

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
    await(runtime.connect(playerId), executor);
    await(runtime.openSession(playerId, roomId), executor);

    Optional<PlayerConnectionSnapshot> disconnected = await(runtime.disconnect(playerId), executor);

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
}
