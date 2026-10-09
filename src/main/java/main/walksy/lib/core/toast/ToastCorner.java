package main.walksy.lib.core.toast;

public enum ToastCorner {

    TOP_LEFT,

    TOP_RIGHT;

    public boolean isRight() {
        return this == TOP_RIGHT;
    }
}
