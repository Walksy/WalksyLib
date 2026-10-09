package main.walksy.lib.core.platform;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.util.function.Consumer;

public final class TinyFileDialogs {
    private TinyFileDialogs() {
    }

    public static void openFile(final String title, final String[] patterns, final String description, final Consumer<String> onPicked) {
        final Window window = Minecraft.getInstance().getWindow();
        final long handle = window.handle();
        final boolean fullscreen = window.isFullscreen();
        if (fullscreen) {
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_AUTO_ICONIFY, GLFW.GLFW_FALSE);
        }
        final Thread thread = new Thread(() -> {
            final String picked;
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                final PointerBuffer filters = stack.mallocPointer(patterns.length);
                for (final String pattern : patterns) {
                    filters.put(stack.UTF8(pattern));
                }
                filters.flip();
                picked = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(title, null, filters, description, false);
            }
            Minecraft.getInstance().execute(() -> {
                if (fullscreen) {
                    GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_AUTO_ICONIFY, GLFW.GLFW_TRUE);
                }
                GLFW.glfwFocusWindow(handle);
                if (picked != null) {
                    onPicked.accept(picked);
                }
            });
        }, "walksylib-file-dialog");
        thread.setDaemon(true);
        thread.start();
    }
}
