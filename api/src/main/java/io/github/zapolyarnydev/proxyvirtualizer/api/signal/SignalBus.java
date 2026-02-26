package io.github.zapolyarnydev.proxyvirtualizer.api.signal;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Publishes and routes signals to subscribers.
 */
public interface SignalBus {
    /**
     * Publishes a signal to subscribed handlers.
     */
    void publish(Signal<?, ?> signal);

    /**
     * Subscribes to all signals.
     */
    SignalSubscription subscribe(SignalHandler<? super Signal<?, ?>> handler);

    /**
     * Subscribes to signals assignable to the provided type.
     */
    default <T extends Signal<?, ?>> SignalSubscription subscribe(
            Class<T> signalType,
            SignalHandler<? super T> handler
    ) {
        Objects.requireNonNull(signalType, "signalType");
        Objects.requireNonNull(handler, "handler");
        return subscribe(signalType, signal -> true, handler);
    }

    /**
     * Subscribes to signals assignable to the provided type and matching the filter.
     */
    <T extends Signal<?, ?>> SignalSubscription subscribe(
            Class<T> signalType,
            Predicate<? super T> filter,
            SignalHandler<? super T> handler
    );
}
