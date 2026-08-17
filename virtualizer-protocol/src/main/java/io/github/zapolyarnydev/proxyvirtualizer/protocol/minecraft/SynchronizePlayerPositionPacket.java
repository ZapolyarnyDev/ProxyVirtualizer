package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;

public record SynchronizePlayerPositionPacket(
    int teleportId,
    double x,
    double y,
    double z,
    double velocityX,
    double velocityY,
    double velocityZ,
    float yaw,
    float pitch,
    int flags)
    implements ClientboundPacket {}
