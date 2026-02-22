package io.github.zapolyarnydev.proxyvirtualizer.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.zapolyarnydev.proxyvirtualizer.api.ProxyVirtualizerApi;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.Connector;
import io.github.zapolyarnydev.proxyvirtualizer.api.registry.ServerContainer;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.Launcher;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.command.VirtualServerCommand;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.connector.InMemoryConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.connector.VelocityConnectorImpl;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.packet.VelocityVirtualPacketSender;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.registry.InMemoryServerContainer;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.server.DefaultVirtualServerLauncher;
import org.slf4j.Logger;

@Plugin(
        id = "proxyvirtualizer",
        name = "ProxyVirtualizer",
        version = "1.0.0"
)
public final class ProxyVirtualizerVelocityPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;

    private final ServerContainer serverContainer;
    private final ConnectionStorage connectionStorage;
    private final VelocityConnectorImpl connector;
    private final VelocityVirtualPacketSender packetSender;
    private final Launcher launcher;
    private final ProxyVirtualizerApi api;

    @Inject
    public ProxyVirtualizerVelocityPlugin(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;

        this.serverContainer = new InMemoryServerContainer();
        this.connectionStorage = new InMemoryConnectionStorage();
        this.connector = new VelocityConnectorImpl(proxyServer, connectionStorage);
        this.packetSender = new VelocityVirtualPacketSender(proxyServer, connectionStorage);
        this.launcher = new DefaultVirtualServerLauncher(proxyServer, serverContainer, connectionStorage, connector);
        this.api = ProxyVirtualizerApi.of(serverContainer);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        proxyServer.getCommandManager().register(
                proxyServer.getCommandManager()
                        .metaBuilder("vserver")
                        .aliases("virtualserver", "vs")
                        .plugin(this)
                        .build(),
                new VirtualServerCommand(serverContainer, launcher, connector, packetSender)
        );
        logger.info("ProxyVirtualizer initialized");
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        connector.disconnect(event.getPlayer());
        connector.forgetPlayer(event.getPlayer());
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Logger getLogger() {
        return logger;
    }

    public ServerContainer getServerContainer() {
        return serverContainer;
    }

    public ConnectionStorage getConnectionStorage() {
        return connectionStorage;
    }

    public Connector getConnector() {
        return connector;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public VelocityVirtualPacketSender getPacketSender() {
        return packetSender;
    }

    public ProxyVirtualizerApi getApi() {
        return api;
    }
}
