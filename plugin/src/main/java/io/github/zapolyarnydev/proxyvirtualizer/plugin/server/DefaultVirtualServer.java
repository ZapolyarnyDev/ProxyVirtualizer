package io.github.zapolyarnydev.proxyvirtualizer.plugin.server;

import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultVirtualServer implements VirtualServer {

    private final String name;
    private final Set<Integer> supportedProtocolVersions = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<Integer, PacketVersionRule>> packetVersions = new ConcurrentHashMap<>();

    public DefaultVirtualServer(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Virtual server name cannot be blank");
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Integer> getSupportedProtocolVersions() {
        return Set.copyOf(supportedProtocolVersions);
    }

    @Override
    public void allowProtocolVersion(int protocolVersion) {
        supportedProtocolVersions.add(protocolVersion);
    }

    @Override
    public void disallowProtocolVersion(int protocolVersion) {
        supportedProtocolVersions.remove(protocolVersion);
    }

    @Override
    public boolean isProtocolVersionSupported(int protocolVersion) {
        return supportedProtocolVersions.isEmpty() || supportedProtocolVersions.contains(protocolVersion);
    }

    @Override
    public PacketVersionRule registerPacketVersion(String packetKey, int protocolVersion, int packetVersion) {
        String normalizedPacketKey = normalizePacketKey(packetKey);
        PacketVersionRule rule = new PacketVersionRule(normalizedPacketKey, protocolVersion, packetVersion);
        packetVersions
                .computeIfAbsent(normalizedPacketKey, ignored -> new ConcurrentHashMap<>())
                .put(protocolVersion, rule);
        return rule;
    }

    @Override
    public Optional<PacketVersionRule> getPacketVersion(String packetKey, int protocolVersion) {
        String normalizedPacketKey = normalizePacketKey(packetKey);
        return Optional.ofNullable(packetVersions.getOrDefault(normalizedPacketKey, Map.of()).get(protocolVersion));
    }

    @Override
    public boolean removePacketVersion(String packetKey, int protocolVersion) {
        String normalizedPacketKey = normalizePacketKey(packetKey);
        Map<Integer, PacketVersionRule> rulesByProtocol = packetVersions.get(normalizedPacketKey);
        if (rulesByProtocol == null) {
            return false;
        }

        boolean removed = rulesByProtocol.remove(protocolVersion) != null;
        if (rulesByProtocol.isEmpty()) {
            packetVersions.remove(normalizedPacketKey, rulesByProtocol);
        }
        return removed;
    }

    @Override
    public Map<String, Set<PacketVersionRule>> getPacketVersionMatrix() {
        Map<String, Set<PacketVersionRule>> snapshot = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<Integer, PacketVersionRule>> entry : packetVersions.entrySet()) {
            snapshot.put(entry.getKey(), Set.copyOf(entry.getValue().values()));
        }
        return Map.copyOf(snapshot);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DefaultVirtualServer that)) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "DefaultVirtualServer{name='" + name + "'}";
    }

    private static String normalizePacketKey(String packetKey) {
        if (packetKey == null || packetKey.isBlank()) {
            throw new IllegalArgumentException("Packet key cannot be blank");
        }
        return packetKey.trim();
    }
}
