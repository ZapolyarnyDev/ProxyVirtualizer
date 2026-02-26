package io.github.zapolyarnydev.proxyvirtualizer.plugin.connector;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.Connector;
import io.github.zapolyarnydev.proxyvirtualizer.api.exception.PlayerAlreadyConnectedException;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.packet.VelocityVirtualPacketSender;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocityConnectorImpl implements Connector {

    private final ProxyServer proxyServer;
    private final ConnectionStorage connectionStorage;
    private final VelocityVirtualPacketSender packetSender;
    private final Map<UUID, RegisteredServer> previousServers = new ConcurrentHashMap<>();

    public VelocityConnectorImpl(
            ProxyServer proxyServer,
            ConnectionStorage connectionStorage,
            VelocityVirtualPacketSender packetSender
    ) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.connectionStorage = Objects.requireNonNull(connectionStorage, "connectionStorage");
        this.packetSender = Objects.requireNonNull(packetSender, "packetSender");
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
        detachBackendIfPossible(player);
        if (!packetSender.bootstrapVoidLimbo(server, player)) {
            connectionStorage.remove(player);
            sendToPreviousServer(player);
            return false;
        }
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
                .map(server -> sendAndLeaveVirtualServer(player, server, false))
                .orElse(false);
    }

    @Override
    public boolean sendToPreviousServer(Player player) {
        Objects.requireNonNull(player, "player");

        RegisteredServer previous = previousServers.get(player.getUniqueId());
        if (previous == null) {
            return false;
        }

        return sendAndLeaveVirtualServer(player, previous, true);
    }

    public void forgetPlayer(Player player) {
        if (player == null) {
            return;
        }
        previousServers.remove(player.getUniqueId());
    }

    private void detachBackendIfPossible(Player player) {
        player.getCurrentServer().ifPresent(currentServer -> {
            try {
                currentServer.getClass().getMethod("disconnect").invoke(currentServer);
            } catch (ReflectiveOperationException ignored) {

            }
        });

        clearConnectedServerReference(player);
    }

    private void clearConnectedServerReference(Player player) {
        try {
            Class<?> playerClass = player.getClass();

            var connectedServerField = playerClass.getDeclaredField("connectedServer");
            connectedServerField.setAccessible(true);
            connectedServerField.set(player, null);

            try {
                var connectionInFlightField = playerClass.getDeclaredField("connectionInFlight");
                connectionInFlightField.setAccessible(true);
                connectionInFlightField.set(player, null);
            } catch (NoSuchFieldException ignored) {

            }

            try {
                playerClass.getMethod("discardChatQueue").invoke(player);
            } catch (ReflectiveOperationException ignored) {

            }
        } catch (ReflectiveOperationException ignored) {

        }
    }

    private boolean send(Player player, RegisteredServer server) {
        if (server == null) {
            return false;
        }
        player.createConnectionRequest(server).fireAndForget();
        return true;
    }

    private boolean sendAndLeaveVirtualServer(Player player, RegisteredServer server, boolean removePreviousOnSuccess) {
        boolean sent = send(player, server);
        if (!sent) {
            return false;
        }

        connectionStorage.remove(player);
        if (removePreviousOnSuccess) {
            previousServers.remove(player.getUniqueId());
        }
        return true;
    }
}
