package main.walksy.lib.api.event.impl;

@FunctionalInterface
public interface MouseButtonInputEvent {
    void button(int button, int action, int modifiers);
}
