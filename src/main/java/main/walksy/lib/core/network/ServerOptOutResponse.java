package main.walksy.lib.core.network;

@FunctionalInterface
public interface ServerOptOutResponse {
    void respond(boolean disabled);
}
