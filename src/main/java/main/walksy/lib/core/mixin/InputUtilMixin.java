package main.walksy.lib.core.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.callback.WindowDropCallback;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InputConstants.class)
public class InputUtilMixin {

    @ModifyArg(
            method = "setupMouseCallbacks",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetDropCallback(JLorg/lwjgl/glfw/GLFWDropCallbackI;)Lorg/lwjgl/glfw/GLFWDropCallback;"),
            index = 1
    )
    private static GLFWDropCallbackI wrapDropCallback(GLFWDropCallbackI originalCallback) {
        return (window, count, names) -> {
            if (originalCallback != null) {
                originalCallback.invoke(window, count, names);
            }
            WindowDropCallback.onFileDrop(count, names);
        };
    }
}
