package main.walksy.lib.api.config.value;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.platform.WLKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class WLKeyMapping {
    private static final List<WLKeyMapping> LOCAL_INSTANCES = new ArrayList<>();
    private static final List<WLKeyMapping> PENDING = new ArrayList<>();
    private final boolean vanilla;
    private final String name;
    private final String category;
    private final KeyMapping keyMapping;
    private final InputConstants.Key localDefaultKey;
    private InputConstants.Key localKey;
    private boolean localDown;
    private boolean localClickPending;

    private WLKeyMapping(final String name, final int defaultKey, final String category, final boolean vanilla) {
        this.name = name;
        this.category = category;
        this.vanilla = vanilla;
        if (vanilla) {
            this.keyMapping = WLKeys.register(name, defaultKey, category);
            this.localDefaultKey = null;
        } else {
            this.keyMapping = null;
            this.localDefaultKey = WLKeys.keyboardKey(defaultKey);
            this.localKey = this.localDefaultKey;
            LOCAL_INSTANCES.add(this);
        }
        attach(this);
    }

    private WLKeyMapping(final InputConstants.Key detachedKey) {
        this.name = null;
        this.category = null;
        this.vanilla = false;
        this.keyMapping = null;
        this.localDefaultKey = detachedKey;
        this.localKey = detachedKey;
    }

    public static WLKeyMapping create(final String name, final int defaultKey, final String category) {
        return new WLKeyMapping(name, defaultKey, category, true);
    }

    public static WLKeyMapping createLocal(final String name, final int defaultKey, final String category) {
        return new WLKeyMapping(name, defaultKey, category, false);
    }

    public static WLKeyMapping detached(final InputConstants.Key key) {
        return new WLKeyMapping(key);
    }

    public static void attachPending() {
        if (PENDING.isEmpty()) {
            return;
        }
        final List<WLKeyMapping> waiting = List.copyOf(PENDING);
        PENDING.clear();
        for (final WLKeyMapping keymapping : waiting) {
            WalksyLib.getInstance().getKeyBinds().register(keymapping);
        }
    }

    private static void attach(final WLKeyMapping keymapping) {
        final WalksyLib lib = WalksyLib.getInstanceOrNull();
        if (lib == null) {
            PENDING.add(keymapping);
            return;
        }
        lib.getKeyBinds().register(keymapping);
    }

    public static void handleLocalKey(final int code, final int action, final int scancode) {
        if (LOCAL_INSTANCES.isEmpty() || WLClient.screen() != null) {
            return;
        }
        final InputConstants.Key pressed = WLKeys.keyOf(code, scancode);
        for (final WLKeyMapping keymapping : LOCAL_INSTANCES) {
            if (keymapping.isUnbound() || !keymapping.localKey.equals(pressed)) {
                continue;
            }
            if (action == InputConstants.PRESS) {
                keymapping.localDown = true;
                keymapping.localClickPending = true;
            } else if (action == InputConstants.RELEASE) {
                keymapping.localDown = false;
            }
        }
    }

    public boolean isVanilla() {
        return this.vanilla;
    }

    public boolean wraps(final KeyMapping mapping) {
        return this.keyMapping != null && this.keyMapping == mapping;
    }

    public String category() {
        return this.category;
    }

    public String name() {
        return this.vanilla ? this.keyMapping.getName() : this.name;
    }

    public boolean isDown() {
        return this.vanilla ? this.keyMapping.isDown() : this.localDown;
    }

    public boolean consumeClick() {
        if (this.vanilla) {
            return this.keyMapping.consumeClick();
        }
        if (!this.localClickPending) {
            return false;
        }
        this.localClickPending = false;
        return true;
    }

    public boolean isHeld() {
        final InputConstants.Key key = this.currentKey();
        if (key == null || key.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return WalksyLib.getInstance().getMouseHandler().isButtonDown(key.getValue());
        }
        return WLKeys.isKeyDown(key.getValue());
    }

    public boolean isUnbound() {
        return this.vanilla ? this.keyMapping.isUnbound() : this.localKey.equals(InputConstants.UNKNOWN);
    }

    public Component displayKeyMessage() {
        return this.vanilla ? this.keyMapping.getTranslatedKeyMessage() : this.localKey.getDisplayName();
    }

    public InputConstants.Key currentKey() {
        return this.vanilla ? WLKeys.boundKey(this.keyMapping) : this.localKey;
    }

    public void rebind(final InputConstants.Key key) {
        if (this.vanilla) {
            this.keyMapping.setKey(key);
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
        } else {
            this.localKey = key;
            this.localDown = false;
            this.localClickPending = false;
        }
    }

    public void resetToDefault() {
        this.rebind(this.vanilla ? this.keyMapping.getDefaultKey() : this.localDefaultKey);
    }

    public void unregister() {
        if (!this.vanilla) {
            LOCAL_INSTANCES.remove(this);
        }
        if (PENDING.remove(this)) {
            return;
        }
        WalksyLib.getInstance().getKeyBinds().unregister(this);
    }
}
