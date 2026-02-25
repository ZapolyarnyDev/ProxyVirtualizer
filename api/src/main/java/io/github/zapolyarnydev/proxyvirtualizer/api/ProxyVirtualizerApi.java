package io.github.zapolyarnydev.proxyvirtualizer.api;

import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.Connector;
import io.github.zapolyarnydev.proxyvirtualizer.api.registry.ServerContainer;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.Launcher;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class ProxyVirtualizerApi {
    private final ServerContainer serverContainer;
    private final Launcher launcher;
    private final Connector connector;
    private final ConnectionStorage connectionStorage;

    public static ProxyVirtualizerApi of(
            ServerContainer serverContainer,
            Launcher launcher,
            Connector connector,
            ConnectionStorage connectionStorage
    ) {
        return new ProxyVirtualizerApi(serverContainer, launcher, connector, connectionStorage);
    }
}
