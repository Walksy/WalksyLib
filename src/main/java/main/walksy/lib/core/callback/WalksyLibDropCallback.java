package main.walksy.lib.core.callback;

import main.walksy.lib.core.utils.log.WalksyLibLogger;
import org.lwjgl.glfw.GLFWDropCallback;

import java.nio.file.InvalidPathException;
import java.util.function.Consumer;

public class WalksyLibDropCallback {

    private static Consumer<String> dropCallback;

    public static void register(final Consumer<String> callback) {
        dropCallback = callback;
    }

    public static void unregister() {
        dropCallback = null;
    }

    public static void onFileDrop(final int count, final long names) {
        if (dropCallback == null) {
            return;
        }
        for (int i = 0; i < count; ++i) {
            final String name = GLFWDropCallback.getName(names, i);

            try {
                dropCallback.accept(name);
            } catch (InvalidPathException e) {
                WalksyLibLogger.err("Failed to parse path " + name + " " + e);
            }
        }
    }
}
