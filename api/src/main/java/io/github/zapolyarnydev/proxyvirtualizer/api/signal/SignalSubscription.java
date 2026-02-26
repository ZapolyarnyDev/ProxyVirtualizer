package io.github.zapolyarnydev.proxyvirtualizer.api.signal;

/**
 * Handle returned when a signal handler is registered.
 */
public interface SignalSubscription extends AutoCloseable {
    /**
     * Unregisters the handler.
     *
     * @return {@code true} if the handler was active and is now removed
     */
    boolean unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
