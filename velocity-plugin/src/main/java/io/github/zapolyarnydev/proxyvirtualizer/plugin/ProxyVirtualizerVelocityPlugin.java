package io.github.zapolyarnydev.proxyvirtualizer.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(id = "proxyvirtualizer", name = "ProxyVirtualizer", version = "1.1.2")
public final class ProxyVirtualizerVelocityPlugin {

  private final Logger logger;

  @Inject
  public ProxyVirtualizerVelocityPlugin(Logger logger) {
    this.logger = logger;
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    logger.info("ProxyVirtualizer initialized");
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    logger.info("ProxyVirtualizer stopped");
  }
}
