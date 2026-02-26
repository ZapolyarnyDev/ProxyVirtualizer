package io.github.zapolyarnydev.proxyvirtualizer.api.signal;

/**
 * A signal produced by the virtual environment when something occurs.
 * Signals are consumed by registered handlers
 *
 * @param <S> source type (who/what caused the signal)
 * @param <P> payload type (details of what happened)
 */
public interface Signal<S, P> {
    S source();
    P payload();
}