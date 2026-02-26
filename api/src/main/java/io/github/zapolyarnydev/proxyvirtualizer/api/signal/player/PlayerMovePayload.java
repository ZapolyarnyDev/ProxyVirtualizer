package io.github.zapolyarnydev.proxyvirtualizer.api.signal.player;

public record PlayerMovePayload(
        double x,
        double y,
        double z,
        boolean onGround,
        boolean horizontalCollision,
        PlayerPacketSignalKind packetKind
) {
}
