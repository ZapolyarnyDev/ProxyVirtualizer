package io.github.zapolyarnydev.proxyvirtualizer.plugin.connector;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryConnectionStorage implements ConnectionStorage {

    private final Map<UUID, VirtualServer> connections = new ConcurrentHashMap<>();

    @Override
    public boolean isInVirtualServer(Player player) {
        Objects.requireNonNull(player, "player");
        return connections.containsKey(player.getUniqueId());
    }

    @Override
    public Optional<VirtualServer> getVirtualServer(Player player) {
        Objects.requireNonNull(player, "player");
        return Optional.ofNullable(connections.get(player.getUniqueId()));
    }

    @Override
    public void register(Player player, VirtualServer virtualServer) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(virtualServer, "virtualServer");
        connections.put(player.getUniqueId(), virtualServer);
    }

    @Override
    public void register(Collection<Player> player, VirtualServer virtualServer) {
        Objects.requireNonNull(player, "player");
        for (Player value : player) {
            register(value, virtualServer);
        }
    }

    @Override
    public boolean remove(Player player) {
        Objects.requireNonNull(player, "player");
        return connections.remove(player.getUniqueId()) != null;
    }

    @Override
    public void remove(Collection<Player> player) {
        Objects.requireNonNull(player, "player");
        for (Player value : player) {
            remove(value);
        }
    }
}
