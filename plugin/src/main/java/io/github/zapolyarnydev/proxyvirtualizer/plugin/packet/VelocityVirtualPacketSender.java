package io.github.zapolyarnydev.proxyvirtualizer.plugin.packet;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.util.Objects;

public final class VelocityVirtualPacketSender {

    private final ProxyServer proxyServer;
    private final ConnectionStorage connectionStorage;

    public VelocityVirtualPacketSender(ProxyServer proxyServer, ConnectionStorage connectionStorage) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.connectionStorage = Objects.requireNonNull(connectionStorage, "connectionStorage");
    }

    public boolean sendKeepAlive(VirtualServer virtualServer, Player player) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.KEEP_ALIVE)) {
            return false;
        }

        try {
            player.getClass().getMethod("sendKeepAlive").invoke(player);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public boolean sendChat(VirtualServer virtualServer, Player player, Component message) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.CHAT)) {
            return false;
        }

        player.sendMessage(message);
        return true;
    }

    public boolean sendActionBar(VirtualServer virtualServer, Player player, Component message) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.ACTION_BAR)) {
            return false;
        }

        player.sendActionBar(message);
        return true;
    }

    public boolean sendTitle(VirtualServer virtualServer, Player player, Component title, Component subtitle) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.TITLE)) {
            return false;
        }

        player.showTitle(Title.title(title, subtitle));
        return true;
    }

    public boolean disconnectClient(VirtualServer virtualServer, Player player, Component reason) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.DISCONNECT)) {
            return false;
        }
        player.disconnect(reason);
        return true;
    }

    public int broadcastKeepAlive(VirtualServer virtualServer) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (sendKeepAlive(virtualServer, player)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastChat(VirtualServer virtualServer, Component message) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (sendChat(virtualServer, player, message)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastActionBar(VirtualServer virtualServer, Component message) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (sendActionBar(virtualServer, player, message)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastTitle(VirtualServer virtualServer, Component title, Component subtitle) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (sendTitle(virtualServer, player, title, subtitle)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastDisconnect(VirtualServer virtualServer, Component reason) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (disconnectClient(virtualServer, player, reason)) {
                sent++;
            }
        }
        return sent;
    }

    private boolean canSend(VirtualServer virtualServer, Player player, String packetKey) {
        Objects.requireNonNull(virtualServer, "virtualServer");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packetKey, "packetKey");

        boolean inTargetVirtualServer = connectionStorage.getVirtualServer(player)
                .map(virtualServer::equals)
                .orElse(false);
        if (!inTargetVirtualServer) {
            return false;
        }

        int protocolVersion = player.getProtocolVersion().getProtocol();
        if (!virtualServer.isProtocolVersionSupported(protocolVersion)) {
            return false;
        }

        return virtualServer.getPacketVersionMatrix().containsKey(packetKey)
                ? virtualServer.getPacketVersion(packetKey, protocolVersion).isPresent()
                : true;
    }
}
