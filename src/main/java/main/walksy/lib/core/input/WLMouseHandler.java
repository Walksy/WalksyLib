package main.walksy.lib.core.input;

import com.mojang.blaze3d.platform.InputConstants;

public final class WLMouseHandler {
    private static final int TRACKED_BUTTONS = 16;
    private final boolean[] buttonsDown = new boolean[TRACKED_BUTTONS];

    public void onButton(final int button, final int action) {
        if (isTracked(button)) {
            this.buttonsDown[button] = action != InputConstants.RELEASE;
        }
    }

    public boolean isButtonDown(final int button) {
        return isTracked(button) && this.buttonsDown[button];
    }

    public boolean isLeftDown() {
        return this.isButtonDown(InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static boolean isTracked(final int button) {
        return button >= 0 && button < TRACKED_BUTTONS;
    }
}
