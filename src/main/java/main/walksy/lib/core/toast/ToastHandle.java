package main.walksy.lib.core.toast;

public final class ToastHandle {
    private final WLToastManager manager;
    private final WLToast toast;

    public ToastHandle(final WLToastManager manager, final WLToast toast) {
        this.manager = manager;
        this.toast = toast;
    }

    public void dismiss() {
        this.manager.dismissHandle(this.toast);
    }

    public boolean isShowing() {
        return this.manager.isHandleShowing(this.toast);
    }
}
