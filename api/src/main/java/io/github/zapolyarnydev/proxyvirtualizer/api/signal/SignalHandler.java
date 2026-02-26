package io.github.zapolyarnydev.proxyvirtualizer.api.signal;

/**
 * A consumer of typed signals.
 *
 * @param <T> signal type
 */
@FunctionalInterface
public interface SignalHandler<T extends Signal<?, ?>> {
    void handle(T signal);
}
