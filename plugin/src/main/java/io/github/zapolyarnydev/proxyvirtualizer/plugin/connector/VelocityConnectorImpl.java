package io.github.zapolyarnydev.proxyvirtualizer.plugin.connector;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.Connector;
import io.github.zapolyarnydev.proxyvirtualizer.api.exception.PlayerAlreadyConnectedException;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocityConnectorImpl implements Connector {

    private final ProxyServer proxyServer;
    private final ConnectionStorage connectionStorage;
    private final Map<UUID, RegisteredServer> previousServers = new ConcurrentHashMap<>();

    public VelocityConnectorImpl(ProxyServer proxyServer, ConnectionStorage connectionStorage) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.connectionStorage = Objects.requireNonNull(connectionStorage, "connectionStorage");
    }

    @Override
    public boolean connect(VirtualServer server, Player player) throws PlayerAlreadyConnectedException {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(player, "player");

        if (connectionStorage.isInVirtualServer(player)) {
            throw new PlayerAlreadyConnectedException(
                    "Player " + player.getUsername() + " is already connected to a virtual server"
            );
        }

        int protocolVersion = player.getProtocolVersion().getProtocol();
        if (!server.isProtocolVersionSupported(protocolVersion)) {
            return false;
        }

        player.getCurrentServer()
                .map(ServerConnection::getServer)
                .ifPresent(serverConnection -> previousServers.put(player.getUniqueId(), serverConnection));

        connectionStorage.register(player, server);
        return true;
    }

    @Override
    public boolean disconnect(Player player) {
        Objects.requireNonNull(player, "player");
        return connectionStorage.remove(player);
    }

    @Override
    public void disconnectAll() {
        for (Player player : proxyServer.getAllPlayers()) {
            if (connectionStorage.isInVirtualServer(player)) {
                connectionStorage.remove(player);
            }
        }
    }

    @Override
    public boolean sendToGameServer(Player player) {
        Objects.requireNonNull(player, "player");

        if (sendToPreviousServer(player)) {
            return true;
        }

        return proxyServer.getAllServers().stream()
                .findFirst()
                .map(server -> send(player, server))
                .orElse(false);
    }

    @Override
    public boolean sendToPreviousServer(Player player) {
        Objects.requireNonNull(player, "player");

        RegisteredServer previous = previousServers.get(player.getUniqueId());
        if (previous == null) {
            return false;
        }

        boolean sent = send(player, previous);
        if (sent) {
            previousServers.remove(player.getUniqueId());
        }
        return sent;
    }

    public void forgetPlayer(Player player) {
        if (player == null) {
            return;
        }
        previousServers.remove(player.getUniqueId());
    }

    private boolean send(Player player, RegisteredServer server) {
        if (server == null) {
            return false;
        }
        player.createConnectionRequest(server).connect();
        return true;
    }
}
