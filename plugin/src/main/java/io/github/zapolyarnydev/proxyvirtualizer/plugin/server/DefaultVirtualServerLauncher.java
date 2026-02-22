package io.github.zapolyarnydev.proxyvirtualizer.plugin.server;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.network.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.Connector;
import io.github.zapolyarnydev.proxyvirtualizer.api.exception.VirtualServerAlreadyLaunchedException;
import io.github.zapolyarnydev.proxyvirtualizer.api.registry.ServerContainer;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.Launcher;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.packet.VirtualPacketKeys;

import java.util.Objects;

public final class DefaultVirtualServerLauncher implements Launcher {

    private final Object lock = new Object();
    private final ProxyServer proxyServer;
    private final ServerContainer serverContainer;
    private final ConnectionStorage connectionStorage;
    private final Connector connector;

    public DefaultVirtualServerLauncher(
            ProxyServer proxyServer,
            ServerContainer serverContainer,
            ConnectionStorage connectionStorage,
            Connector connector
    ) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.serverContainer = Objects.requireNonNull(serverContainer, "serverContainer");
        this.connectionStorage = Objects.requireNonNull(connectionStorage, "connectionStorage");
        this.connector = Objects.requireNonNull(connector, "connector");
    }

    @Override
    public VirtualServer launch(String name) throws VirtualServerAlreadyLaunchedException {
        synchronized (lock) {
            if (serverContainer.findServerByName(name).isPresent()) {
                throw new VirtualServerAlreadyLaunchedException("Virtual server already launched: " + name);
            }

            VirtualServer virtualServer = new DefaultVirtualServer(name);
            int targetProtocol = ProtocolVersion.MINECRAFT_1_21_4.getProtocol();
            virtualServer.allowProtocolVersion(targetProtocol);
            virtualServer.registerPacketVersion(VirtualPacketKeys.LIMBO_BOOTSTRAP, targetProtocol, 1);
            virtualServer.registerPacketVersion(VirtualPacketKeys.RESPAWN, targetProtocol, 1);
            virtualServer.registerPacketVersion(VirtualPacketKeys.KEEP_ALIVE, targetProtocol, 1);
            virtualServer.registerPacketVersion(VirtualPacketKeys.CHAT, targetProtocol, 1);
            virtualServer.registerPacketVersion(VirtualPacketKeys.ACTION_BAR, targetProtocol, 1);
            virtualServer.registerPacketVersion(VirtualPacketKeys.TITLE, targetProtocol, 1);
            virtualServer.registerPacketVersion(VirtualPacketKeys.DISCONNECT, targetProtocol, 1);
            serverContainer.register(virtualServer);
            return virtualServer;
        }
    }

    @Override
    public void stop(String name) {
        synchronized (lock) {
            VirtualServer virtualServer = serverContainer.findServerByName(name).orElse(null);
            if (virtualServer == null) {
                return;
            }

            for (Player player : proxyServer.getAllPlayers()) {
                boolean isConnectedToTarget = connectionStorage.getVirtualServer(player)
                        .map(virtualServer::equals)
                        .orElse(false);

                if (!isConnectedToTarget) {
                    continue;
                }

                connector.disconnect(player);
                connector.sendToPreviousServer(player);
            }

            serverContainer.remove(virtualServer);
        }
    }
}
