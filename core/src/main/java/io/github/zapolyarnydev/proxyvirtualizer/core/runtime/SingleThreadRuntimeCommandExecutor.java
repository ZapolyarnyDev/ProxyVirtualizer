package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.jetbrains.annotations.NotNull;

public final class SingleThreadRuntimeCommandExecutor implements RuntimeCommandExecutor {

  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          Thread.ofPlatform().name("proxyvirtualizer-runtime-", 0).factory());

  @Override
  @NotNull
  public <T> CompletionStage<T> submit(@NotNull Callable<? extends T> command) {
    Objects.requireNonNull(command, "command");
    CompletableFuture<T> result = new CompletableFuture<>();

    try {
      executor.execute(
          () -> {
            try {
              result.complete(command.call());
            } catch (Throwable throwable) {
              result.completeExceptionally(throwable);
            }
          });
    } catch (RejectedExecutionException exception) {
      result.completeExceptionally(exception);
    }

    return result;
  }

  @Override
  public void close() {
    executor.shutdown();
  }
}
