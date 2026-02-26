package io.github.zapolyarnydev.proxyvirtualizer.api.signal.player;

public record PlayerLookPayload(
        float yaw,
        float pitch,
        boolean onGround,
        boolean horizontalCollision,
        PlayerPacketSignalKind packetKind
) {
}
