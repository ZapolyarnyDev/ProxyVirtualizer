package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.executor.RuntimeCommandExecutor;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.executor.SingleThreadRuntimeCommandExecutor;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.dispatch.RuntimeSignalDispatcher;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.PlayerConnectionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.RoomSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.SessionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.NotNull;

public final class VirtualizerRuntime implements PlayerConnectionLifecycle, AutoCloseable {

  private final Clock clock;
  private final RuntimeCommandExecutor executor;
  private final RuntimeSignalDispatcher signalDispatcher;
  private final VirtualizerRuntimeState state = new VirtualizerRuntimeState();

  public VirtualizerRuntime() {
    this(
        Clock.systemUTC(),
        new SingleThreadRuntimeCommandExecutor(),
        RuntimeSignalDispatcher.noop());
  }

  public VirtualizerRuntime(@NotNull RuntimeSignalDispatcher signalDispatcher) {
    this(Clock.systemUTC(), new SingleThreadRuntimeCommandExecutor(), signalDispatcher);
  }

  public VirtualizerRuntime(@NotNull Clock clock, @NotNull RuntimeCommandExecutor executor) {
    this(clock, executor, RuntimeSignalDispatcher.noop());
  }

  public VirtualizerRuntime(
      @NotNull Clock clock,
      @NotNull RuntimeCommandExecutor executor,
      @NotNull RuntimeSignalDispatcher signalDispatcher) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.executor = Objects.requireNonNull(executor, "executor");
    this.signalDispatcher = Objects.requireNonNull(signalDispatcher, "signalDispatcher");
  }

  @NotNull
  public CompletionStage<RoomSnapshot> registerRoom(@NotNull RoomId roomId) {
    Objects.requireNonNull(roomId, "roomId");
    return submit(() -> state.registerRoom(roomId));
  }

  @NotNull
  public CompletionStage<List<RoomSnapshot>> rooms() {
    return executor.submit(state::rooms);
  }

  @NotNull
  public CompletionStage<Optional<RoomSnapshot>> findRoom(@NotNull RoomId roomId) {
    Objects.requireNonNull(roomId, "roomId");
    return executor.submit(() -> state.findRoom(roomId));
  }

  @NotNull
  public CompletionStage<PlayerConnectionSnapshot> connect(
      @NotNull PlayerId playerId, @NotNull ConnectionId connectionId) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(connectionId, "connectionId");
    return submit(() -> state.connect(playerId, connectionId));
  }

  @NotNull
  public CompletionStage<Optional<PlayerConnectionSnapshot>> findConnection(
      @NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return executor.submit(() -> state.findConnection(playerId));
  }

  @NotNull
  public CompletionStage<SessionSnapshot> openSession(
      @NotNull PlayerId playerId, @NotNull RoomId roomId) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(roomId, "roomId");
    return submit(() -> state.openSession(playerId, roomId, clock));
  }

  @NotNull
  public CompletionStage<Optional<SessionSnapshot>> findSession(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return executor.submit(() -> state.findSession(playerId));
  }

  @NotNull
  public CompletionStage<Optional<SessionSnapshot>> closeSession(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return submit(() -> state.closeSession(playerId));
  }

  @NotNull
  public CompletionStage<Optional<PlayerConnectionSnapshot>> disconnect(
      @NotNull ConnectionId connectionId) {
    Objects.requireNonNull(connectionId, "connectionId");
    return submit(() -> state.disconnect(connectionId));
  }

  @Override
  public CompletionStage<Void> playerConnected(
      @NotNull PlayerId playerId, @NotNull ConnectionId connectionId) {
    return connect(playerId, connectionId).thenApply(connection -> null);
  }

  @Override
  public CompletionStage<Void> playerDisconnected(@NotNull ConnectionId connectionId) {
    return disconnect(connectionId).thenApply(connection -> null);
  }

  @Override
  public void close() {
    executor.close();
  }

  private <T> CompletionStage<T> submit(Callable<RuntimeTransition<T>> command) {
    return executor.submit(
        () -> {
          RuntimeTransition<T> transition = command.call();
          transition.signals().forEach(signalDispatcher::dispatch);
          return transition.value();
        });
  }
}
