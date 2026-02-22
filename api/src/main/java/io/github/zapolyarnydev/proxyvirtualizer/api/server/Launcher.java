package io.github.zapolyarnydev.proxyvirtualizer.api.server;

import io.github.zapolyarnydev.proxyvirtualizer.api.exception.VirtualServerAlreadyLaunchedException;

public interface Launcher {

    VirtualServer launch(String name) throws VirtualServerAlreadyLaunchedException;

    void stop(String name);
}
