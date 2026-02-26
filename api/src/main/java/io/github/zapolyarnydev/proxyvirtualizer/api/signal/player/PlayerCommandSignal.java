package io.github.zapolyarnydev.proxyvirtualizer.api.signal.player;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.Signal;

public record PlayerCommandSignal(Player source, PlayerCommandPayload payload)
        implements Signal<Player, PlayerCommandPayload> {
}
