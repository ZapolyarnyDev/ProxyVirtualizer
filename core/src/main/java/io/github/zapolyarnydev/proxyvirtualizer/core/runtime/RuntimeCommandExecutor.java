package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.NotNull;

public interface RuntimeCommandExecutor extends AutoCloseable {

  @NotNull
  <T> CompletionStage<T> submit(@NotNull Callable<? extends T> command);

  @Override
  void close();
}
