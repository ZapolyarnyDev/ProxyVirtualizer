package io.github.zapolyarnydev.proxyvirtualizer.plugin.registry;

import io.github.zapolyarnydev.proxyvirtualizer.api.registry.ServerContainer;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryServerContainer implements ServerContainer {

    private final Map<String, VirtualServer> serversByName = new ConcurrentHashMap<>();

    @Override
    public Set<VirtualServer> getServers() {
        return Set.copyOf(new LinkedHashSet<>(serversByName.values()));
    }

    @Override
    public void register(VirtualServer virtualServer) {
        Objects.requireNonNull(virtualServer, "virtualServer");

        String key = normalize(virtualServer.getName());
        VirtualServer previous = serversByName.putIfAbsent(key, virtualServer);
        if (previous != null) {
            throw new IllegalStateException("Virtual server already registered: " + virtualServer.getName());
        }
    }

    @Override
    public void remove(VirtualServer virtualServer) {
        if (virtualServer == null) {
            return;
        }

        serversByName.remove(normalize(virtualServer.getName()), virtualServer);
    }

    @Override
    public Optional<VirtualServer> findServerByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(serversByName.get(normalize(name)));
    }

    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Virtual server name cannot be blank");
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
