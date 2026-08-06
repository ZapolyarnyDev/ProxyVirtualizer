package io.github.zapolyarnydev.proxyvirtualizer.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.VirtualizerRuntime;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityPlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityPlayerConnectionListener;
import org.slf4j.Logger;

@Plugin(id = "proxyvirtualizer", name = "ProxyVirtualizer", version = "1.1.2")
public final class ProxyVirtualizerVelocityPlugin {

  private final ProxyServer server;
  private final Logger logger;
  private VirtualizerRuntime runtime;
  private VelocityPlayerConnectionListener playerConnectionListener;

  @Inject
  public ProxyVirtualizerVelocityPlugin(ProxyServer server, Logger logger) {
    this.server = server;
    this.logger = logger;
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    runtime = new VirtualizerRuntime();

    var lifecycle = new VelocityPlayerConnectionLifecycle(runtime);
    playerConnectionListener = new VelocityPlayerConnectionListener(lifecycle);

    server.getEventManager().register(this, playerConnectionListener);
    logger.info("ProxyVirtualizer initialized");
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    if (runtime != null) runtime.close();
    logger.info("ProxyVirtualizer stopped");
  }
}
