package main.walksy.lib.core.callback;

import main.walksy.lib.core.utils.log.WalksyLibLogger;
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
        GLFWDropCallback rtrn = GLFW.glfwSetDropCallback(windowHandle, glfwDropCallback);
        WalksyLibLogger.info("Attempted to register GLFW Drop Callback: " + rtrn);
    }

    public static void unregister(long windowHandle) {
        dropCallback = null;
        GLFWDropCallback rtrn = GLFW.glfwSetDropCallback(windowHandle, null); //TODO Test
        WalksyLibLogger.info("Attempted to un-register GLFW Drop Callback: " + rtrn);
    }

    private static void onFileDrop(String path) {
        if (dropCallback != null) {
            dropCallback.accept(path);
        }
    }
}
