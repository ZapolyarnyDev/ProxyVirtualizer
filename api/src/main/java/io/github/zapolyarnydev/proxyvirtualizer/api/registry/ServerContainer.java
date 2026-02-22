package io.github.zapolyarnydev.proxyvirtualizer.api.registry;

import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;

import java.util.Optional;
import java.util.Set;

public interface ServerContainer {

    Set<VirtualServer> getServers();

    void register(VirtualServer virtualServer);

    void remove(VirtualServer virtualServer);

    Optional<VirtualServer> findServerByName(String name);
}
