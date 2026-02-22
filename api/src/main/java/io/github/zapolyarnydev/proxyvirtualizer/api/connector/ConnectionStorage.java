package io.github.zapolyarnydev.proxyvirtualizer.api.connector;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;

import java.util.Collection;
import java.util.Optional;

public interface ConnectionStorage {

    boolean isInVirtualServer(Player player);

    Optional<VirtualServer> getVirtualServer(Player player);

    void register(Player player, VirtualServer virtualServer);

    void register(Collection<Player> player, VirtualServer virtualServer);

    boolean remove(Player player);

    void remove(Collection<Player> player);
}
