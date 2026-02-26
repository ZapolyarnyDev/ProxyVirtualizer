package io.github.zapolyarnydev.proxyvirtualizer.api.signal.impl;

import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.Signal;


public record InboundPlayerSignal<P>(Player source, P payload) implements Signal<Player, P> { }
