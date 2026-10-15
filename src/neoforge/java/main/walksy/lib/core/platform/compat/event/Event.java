package main.walksy.lib.core.platform.compat.event;

public abstract class Event<T> {
    protected Event() {
    }

    public abstract T invoker();

    public abstract void register(T listener);
}
