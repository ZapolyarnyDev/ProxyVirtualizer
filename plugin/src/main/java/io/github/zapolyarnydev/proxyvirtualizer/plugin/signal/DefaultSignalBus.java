package io.github.zapolyarnydev.proxyvirtualizer.plugin.signal;

import io.github.zapolyarnydev.proxyvirtualizer.api.signal.Signal;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.SignalBus;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.SignalHandler;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.SignalSubscription;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public final class DefaultSignalBus implements SignalBus {
    @SuppressWarnings("rawtypes")
    private static final Class GLOBAL_SIGNAL_TYPE = Signal.class;

    private final Logger logger;
    private final CopyOnWriteArrayList<SubscriptionImpl<?>> subscriptions = new CopyOnWriteArrayList<>();

    public DefaultSignalBus(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void publish(Signal<?, ?> signal) {
        Objects.requireNonNull(signal, "signal");
        for (SubscriptionImpl<?> subscription : subscriptions) {
            subscription.tryHandle(signal);
        }
    }

    @Override
    public SignalSubscription subscribe(SignalHandler<? super Signal<?, ?>> handler) {
        Objects.requireNonNull(handler, "handler");
        @SuppressWarnings("unchecked")
        Class<Signal<?, ?>> type = (Class<Signal<?, ?>>) GLOBAL_SIGNAL_TYPE;
        return subscribe(type, signal -> true, handler);
    }

    @Override
    public <T extends Signal<?, ?>> SignalSubscription subscribe(
            Class<T> signalType,
            Predicate<? super T> filter,
            SignalHandler<? super T> handler
    ) {
        Objects.requireNonNull(signalType, "signalType");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(handler, "handler");

        SubscriptionImpl<T> subscription = new SubscriptionImpl<>(signalType, filter, handler);
        subscriptions.add(subscription);
        return subscription;
    }

    private final class SubscriptionImpl<T extends Signal<?, ?>> implements SignalSubscription {
        private final Class<T> signalType;
        private final Predicate<? super T> filter;
        private final SignalHandler<? super T> handler;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private SubscriptionImpl(
                Class<T> signalType,
                Predicate<? super T> filter,
                SignalHandler<? super T> handler
        ) {
            this.signalType = signalType;
            this.filter = filter;
            this.handler = handler;
        }

        @Override
        public boolean unsubscribe() {
            if (!active.compareAndSet(true, false)) {
                return false;
            }
            subscriptions.remove(this);
            return true;
        }

        private void tryHandle(Signal<?, ?> signal) {
            if (!active.get() || !signalType.isInstance(signal)) {
                return;
            }

            T typedSignal = signalType.cast(signal);
            try {
                if (!filter.test(typedSignal)) {
                    return;
                }
                handler.handle(typedSignal);
            } catch (Throwable throwable) {
                logger.warn("Signal handler failed for {}", signal.getClass().getName(), throwable);
            }
        }
    }
}
