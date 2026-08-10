package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.VirtualizerRuntime;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.dispatch.RuntimeSignalBus;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityConnectionRegistry;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityPlayerConnectionLifecycle;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityPlayerConnectionListener;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.netty.transport.VelocitySessionTransport;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination.VelocitySessionTransportCoordinator;
import org.slf4j.Logger;

@Plugin(id = "proxyvirtualizer", name = "ProxyVirtualizer", version = "1.1.2")
public final class ProxyVirtualizerVelocityPlugin {

  private final ProxyServer server;
  private final Logger logger;
  private VirtualizerRuntime runtime;
  private RuntimeSignalBus signalBus;
  private VelocitySessionTransportCoordinator transportCoordinator;
  private VelocityPlayerConnectionListener playerConnectionListener;

  @Inject
  public ProxyVirtualizerVelocityPlugin(ProxyServer server, Logger logger) {
    this.server = server;
    this.logger = logger;
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    signalBus =
        new RuntimeSignalBus(
            (listener, signal, cause) -> logger.error("Runtime signal listener failed", cause));
    runtime = new VirtualizerRuntime(signalBus);

    var connections = new VelocityConnectionRegistry();
    var lifecycle = new VelocityPlayerConnectionLifecycle(runtime, connections);
    transportCoordinator =
        new VelocitySessionTransportCoordinator(
            connections,
            VelocitySessionTransport::create,
            runtime::closeSession,
            (session, cause) ->
                logger.error("Session transport failed for " + session.id(), cause));
    playerConnectionListener =
        new VelocityPlayerConnectionListener(
            lifecycle, cause -> logger.error("Player connection lifecycle failed", cause));

    signalBus.subscribe(transportCoordinator);
    server.getEventManager().register(this, playerConnectionListener);
    logger.info("ProxyVirtualizer initialized");
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    if (signalBus != null && transportCoordinator != null)
      signalBus.unsubscribe(transportCoordinator);
    if (transportCoordinator != null) transportCoordinator.close();
    if (runtime != null) runtime.close();
    logger.info("ProxyVirtualizer stopped");
  }
}
