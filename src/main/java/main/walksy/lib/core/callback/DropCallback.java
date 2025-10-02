package main.walksy.lib.core.callback;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;

import java.util.function.Consumer;

public class DropCallback {

    private Consumer<String> dropCallback;

    public void register(long windowHandle, Consumer<String> callback) {
        this.dropCallback = callback;

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

    public void unregister(long windowHandle) {
        dropCallback = null;
    }

    private void onFileDrop(String path) {
        if (dropCallback != null) {
            dropCallback.accept(path);
        }
    }
}
