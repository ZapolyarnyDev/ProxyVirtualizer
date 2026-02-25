package io.github.zapolyarnydev.proxyvirtualizer.api;

import java.util.Objects;
import java.util.Optional;

public final class ProxyVirtualizerApiProvider {
    private static volatile ProxyVirtualizerApi api;

    private ProxyVirtualizerApiProvider() {
    }

    public static Optional<ProxyVirtualizerApi> get() {
        return Optional.ofNullable(api);
    }

    public static ProxyVirtualizerApi require() {
        ProxyVirtualizerApi current = api;
        if (current == null) {
            throw new IllegalStateException("ProxyVirtualizer API is not registered");
        }
        return current;
    }

    public static void register(ProxyVirtualizerApi api) {
        ProxyVirtualizerApiProvider.api = Objects.requireNonNull(api, "api");
    }

    public static void unregister() {
        api = null;
    }
}
