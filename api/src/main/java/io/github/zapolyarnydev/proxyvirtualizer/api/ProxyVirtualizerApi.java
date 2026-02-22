package io.github.zapolyarnydev.proxyvirtualizer.api;

import io.github.zapolyarnydev.proxyvirtualizer.api.registry.ServerContainer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class ProxyVirtualizerApi {
    private final ServerContainer serverContainer;

    public static ProxyVirtualizerApi of(ServerContainer serverContainer) {
        return new ProxyVirtualizerApi(serverContainer);
    }
}
