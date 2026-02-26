package io.github.zapolyarnydev.proxyvirtualizer.api.signal.player;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.Signal;

public record PlayerMoveSignal(Player source, PlayerMovePayload payload)
        implements Signal<Player, PlayerMovePayload> {
}
