package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.port;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.SessionSnapshot;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SessionTransportFailureHandler {

  void onFailure(@NotNull SessionSnapshot session, @NotNull Throwable cause);
}
