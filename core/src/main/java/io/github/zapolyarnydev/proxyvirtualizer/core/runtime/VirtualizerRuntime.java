package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import io.github.zapolyarnydev.proxyvirtualizer.core.connection.PlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.NotNull;

public final class VirtualizerRuntime implements PlayerConnectionLifecycle, AutoCloseable {

  private final Clock clock;
  private final RuntimeCommandExecutor executor;
  private final VirtualizerRuntimeState state = new VirtualizerRuntimeState();

  public VirtualizerRuntime() {
    this(Clock.systemUTC(), new SingleThreadRuntimeCommandExecutor());
  }

  public VirtualizerRuntime(@NotNull Clock clock, @NotNull RuntimeCommandExecutor executor) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.executor = Objects.requireNonNull(executor, "executor");
  }

  @NotNull
  public CompletionStage<RoomSnapshot> registerRoom(@NotNull RoomId roomId) {
    Objects.requireNonNull(roomId, "roomId");
    return executor.submit(() -> state.registerRoom(roomId));
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
  public CompletionStage<PlayerConnectionSnapshot> connect(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return executor.submit(() -> state.connect(playerId));
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
    return executor.submit(() -> state.openSession(playerId, roomId, clock));
  }

  @NotNull
  public CompletionStage<Optional<SessionSnapshot>> findSession(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return executor.submit(() -> state.findSession(playerId));
  }

  @NotNull
  public CompletionStage<Optional<SessionSnapshot>> closeSession(@NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return executor.submit(() -> state.closeSession(playerId));
  }

  @NotNull
  public CompletionStage<Optional<PlayerConnectionSnapshot>> disconnect(
      @NotNull PlayerId playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return executor.submit(() -> state.disconnect(playerId));
  }

  @Override
  public void playerConnected(@NotNull PlayerId playerId) {
    connect(playerId);
  }

  @Override
  public void playerDisconnected(@NotNull PlayerId playerId) {
    disconnect(playerId);
  }

  @Override
  public void close() {
    executor.close();
  }
}
