package io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public final class VelocityPlayerConnectionListener {

  private final VelocityPlayerConnectionLifecycle lifecycle;
  private final Consumer<Throwable> failureHandler;

  public VelocityPlayerConnectionListener(@NotNull VelocityPlayerConnectionLifecycle lifecycle) {
    this(lifecycle, ignored -> {});
  }

  public VelocityPlayerConnectionListener(
      @NotNull VelocityPlayerConnectionLifecycle lifecycle,
      @NotNull Consumer<Throwable> failureHandler) {
    this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
  }

  @Subscribe
  public void onPostLogin(PostLoginEvent event) {
    observe(() -> lifecycle.playerConnected(event.getPlayer()));
  }

  @Subscribe
  public void onDisconnect(DisconnectEvent event) {
    observe(() -> lifecycle.playerDisconnected(event.getPlayer()));
  }

  void observe(Supplier<? extends CompletionStage<Void>> operation) {
    CompletionStage<Void> result;
    try {
      result = Objects.requireNonNull(operation.get(), "lifecycle result");
    } catch (RuntimeException cause) {
      failureHandler.accept(cause);
      return;
    }
    result.whenComplete(
        (ignored, cause) -> {
          if (cause != null) failureHandler.accept(cause);
        });
  }
}
