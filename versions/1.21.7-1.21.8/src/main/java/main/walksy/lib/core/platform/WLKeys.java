package main.walksy.lib.core.platform;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class WLKeys {
    private WLKeys() {
    }

    public static KeyMapping register(final String name, final int defaultKey, final String category) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(name, InputConstants.Type.KEYSYM, defaultKey, category));
    }

    public static InputConstants.Key boundKey(final KeyMapping mapping) {
        return KeyBindingHelper.getBoundKeyOf(mapping);
    }

    public static InputConstants.Key keyboardKey(final int code) {
        return InputConstants.Type.KEYSYM.getOrCreate(code);
    }

    public static InputConstants.Key keyOf(final int code, final int scancode) {
        return code == InputConstants.UNKNOWN.getValue() ? InputConstants.Type.SCANCODE.getOrCreate(scancode) : InputConstants.Type.KEYSYM.getOrCreate(code);
    }

    public static boolean isKeyDown(final int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().handle(), key);
    }
}
