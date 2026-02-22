package io.github.zapolyarnydev.proxyvirtualizer.api.server;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface VirtualServer {
    String getName();

    Set<Integer> getSupportedProtocolVersions();

    void allowProtocolVersion(int protocolVersion);

    void disallowProtocolVersion(int protocolVersion);

    boolean isProtocolVersionSupported(int protocolVersion);

    PacketVersionRule registerPacketVersion(String packetKey, int protocolVersion, int packetVersion);

    Optional<PacketVersionRule> getPacketVersion(String packetKey, int protocolVersion);

    boolean removePacketVersion(String packetKey, int protocolVersion);

    Map<String, Set<PacketVersionRule>> getPacketVersionMatrix();

    record PacketVersionRule(String packetKey, int protocolVersion, int packetVersion) {
    }
}
