package main.walksy.lib.core.platform.compat.event;

import java.lang.reflect.Array;
import java.util.function.Function;

public final class EventFactory {
    private EventFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Event<T> createArrayBacked(final Class<? super T> type, final Function<T[], T> invokerFactory) {
        return new ArrayBackedEvent<>((Class<T>) type, invokerFactory);
    }

    private static final class ArrayBackedEvent<T> extends Event<T> {
        private final Class<T> type;
        private final Function<T[], T> invokerFactory;
        private volatile T[] listeners;
        private volatile T invoker;

        @SuppressWarnings("unchecked")
        private ArrayBackedEvent(final Class<T> type, final Function<T[], T> invokerFactory) {
            this.type = type;
            this.invokerFactory = invokerFactory;
            this.listeners = (T[]) Array.newInstance(type, 0);
            this.invoker = invokerFactory.apply(this.listeners);
        }

        @Override
        public T invoker() {
            return this.invoker;
        }

        @Override
        @SuppressWarnings("unchecked")
        public synchronized void register(final T listener) {
            final T[] next = (T[]) Array.newInstance(this.type, this.listeners.length + 1);
            System.arraycopy(this.listeners, 0, next, 0, this.listeners.length);
            next[this.listeners.length] = listener;
            this.listeners = next;
            this.invoker = this.invokerFactory.apply(next);
        }
    }
}
