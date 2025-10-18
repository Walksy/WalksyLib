package main.walksy.lib.core.callback;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;

import java.util.function.Consumer;

public class WindowDropCallback {

    private static Consumer<String> dropCallback;

    public static void register(long windowHandle, Consumer<String> callback) {
        dropCallback = callback;

        GLFWDropCallback glfwDropCallback = new GLFWDropCallback() {
            @Override
            public void invoke(long window, int count, long names) {
                for (int i = 0; i < count; i++) {
                    String path = getName(names, i);
                    onFileDrop(path);
                }
            }
        };
        GLFW.glfwSetDropCallback(windowHandle, glfwDropCallback);
    }

    public static void unregister() {
        dropCallback = null;
    }

    private static void onFileDrop(String path) {
        if (dropCallback != null) {
            dropCallback.accept(path);
        }
    }
}
