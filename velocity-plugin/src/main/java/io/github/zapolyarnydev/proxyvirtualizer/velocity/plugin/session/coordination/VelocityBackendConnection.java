package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import java.util.concurrent.CompletionStage;

interface VelocityBackendConnection {

  CompletionStage<Void> restore();

  void terminate();
}
