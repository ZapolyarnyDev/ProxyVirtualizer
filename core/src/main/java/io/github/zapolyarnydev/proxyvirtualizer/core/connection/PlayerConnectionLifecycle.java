package io.github.zapolyarnydev.proxyvirtualizer.core.connection;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import org.jetbrains.annotations.NotNull;

public interface PlayerConnectionLifecycle {

  void playerConnected(@NotNull PlayerId playerId);

  void playerDisconnected(@NotNull PlayerId playerId);
}
