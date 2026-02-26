package io.github.zapolyarnydev.proxyvirtualizer.api.signal.player;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.Signal;

public record PlayerChatSignal(Player source, PlayerChatPayload payload)
        implements Signal<Player, PlayerChatPayload> {
}
