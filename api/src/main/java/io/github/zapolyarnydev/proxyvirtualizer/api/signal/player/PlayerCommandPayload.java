package io.github.zapolyarnydev.proxyvirtualizer.api.signal.player;

import java.util.List;

public record PlayerCommandPayload(
        String rawCommand,
        String label,
        List<String> arguments,
        String invocationSource,
        String signedState
) {
}
