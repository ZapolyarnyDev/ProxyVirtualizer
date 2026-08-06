package io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal;

public sealed interface RuntimeSignal
    permits PlayerConnectedSignal,
        PlayerDisconnectedSignal,
        RoomRegisteredSignal,
        SessionClosedSignal,
        SessionOpenedSignal {}
