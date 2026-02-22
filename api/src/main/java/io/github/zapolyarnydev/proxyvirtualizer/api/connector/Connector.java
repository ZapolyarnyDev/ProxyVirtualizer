package io.github.zapolyarnydev.proxyvirtualizer.api.connector;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.exception.PlayerAlreadyConnectedException;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;

public interface Connector {
    boolean connect(VirtualServer server, Player player) throws PlayerAlreadyConnectedException;

    boolean disconnect(Player player);

    void disconnectAll();

    boolean sendToGameServer(Player player);

    boolean sendToPreviousServer(Player player);

}
