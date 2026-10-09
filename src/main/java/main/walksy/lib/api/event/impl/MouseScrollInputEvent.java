package main.walksy.lib.api.event.impl;

@FunctionalInterface
public interface MouseScrollInputEvent {
    void scroll(double xOffset, double yOffset);
}
