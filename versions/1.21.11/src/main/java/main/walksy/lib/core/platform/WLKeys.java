package main.walksy.lib.core.platform;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.WalksyLibConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class WLKeys {
    private static final Map<String, KeyMapping.Category> CATEGORIES = new HashMap<>();
    private static final Map<KeyMapping.Category, String> CATEGORY_NAMES = new HashMap<>();

    private WLKeys() {
    }

    public static KeyMapping register(final String name, final int defaultKey, final String category) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(name, InputConstants.Type.KEYSYM, defaultKey, categoryFor(category)));
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
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }

    public static String categoryName(final KeyMapping.Category category) {
        return CATEGORY_NAMES.get(category);
    }

    private static KeyMapping.Category categoryFor(final String name) {
        final String sanitized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return CATEGORIES.computeIfAbsent(sanitized, path -> {
            final KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(WalksyLibConstants.NAMESPACE, path));
            CATEGORY_NAMES.put(category, name);
            return category;
        });
    }
}
