package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ServerboundPacket;

public record ServerboundKeepAlivePacket(long id) implements ServerboundPacket {}
