package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft.packet;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.model.ServerboundPacket;

public record ServerboundKeepAlivePacket(long id) implements ServerboundPacket {}
