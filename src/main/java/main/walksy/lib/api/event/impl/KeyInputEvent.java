package main.walksy.lib.api.event.impl;

@FunctionalInterface
public interface KeyInputEvent {
    void key(int code, int action, int scancode, int modifiers);
}
