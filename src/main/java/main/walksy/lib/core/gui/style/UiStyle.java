package main.walksy.lib.core.gui.style;

import main.walksy.lib.core.WalksyLib;

public enum UiStyle {
    MODERN,
    MINECRAFT;

    public static UiStyle current() {
        final WalksyLib lib = WalksyLib.getInstance();
        if (lib == null || lib.getAppearancePage() == null) {
            return MODERN;
        }
        return lib.getAppearancePage().style();
    }

    public static boolean minecraft() {
        return current() == MINECRAFT;
    }
}
