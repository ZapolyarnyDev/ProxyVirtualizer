package io.github.zapolyarnydev.proxyvirtualizer.api.signal.player;

/**
 * Serverbound packet variants that can carry player movement / look updates.
 */
public enum PlayerPacketSignalKind {
    POSITION,
    POSITION_AND_ROTATION,
    ROTATION
}
