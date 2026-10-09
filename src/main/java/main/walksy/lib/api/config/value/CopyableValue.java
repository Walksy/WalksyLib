package main.walksy.lib.api.config.value;

public interface CopyableValue<T extends CopyableValue<T>> {
    T copy();
}
