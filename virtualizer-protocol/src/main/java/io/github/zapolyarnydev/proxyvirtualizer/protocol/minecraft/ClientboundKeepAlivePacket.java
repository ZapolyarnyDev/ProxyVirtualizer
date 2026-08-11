package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ClientboundPacket;

public record ClientboundKeepAlivePacket(long id) implements ClientboundPacket {}
