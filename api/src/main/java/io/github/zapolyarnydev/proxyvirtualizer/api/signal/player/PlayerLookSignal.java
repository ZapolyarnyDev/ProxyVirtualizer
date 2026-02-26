package io.github.zapolyarnydev.proxyvirtualizer.api.signal.player;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.Signal;

public record PlayerLookSignal(Player source, PlayerLookPayload payload)
        implements Signal<Player, PlayerLookPayload> {
}
