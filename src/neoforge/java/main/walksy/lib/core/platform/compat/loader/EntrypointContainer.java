package main.walksy.lib.core.platform.compat.loader;

public final class EntrypointContainer<T> {
    private final T entrypoint;
    private final ModContainer provider;

    EntrypointContainer(final T entrypoint, final ModContainer provider) {
        this.entrypoint = entrypoint;
        this.provider = provider;
    }

    public T getEntrypoint() {
        return this.entrypoint;
    }

    public ModContainer getProvider() {
        return this.provider;
    }
}
