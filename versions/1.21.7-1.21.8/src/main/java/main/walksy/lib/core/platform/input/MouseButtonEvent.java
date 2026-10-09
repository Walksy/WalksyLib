package main.walksy.lib.core.platform.input;

public record MouseButtonEvent(double x, double y, MouseButtonInfo buttonInfo) implements InputWithModifiers {
    @Override
    public int input() {
        return this.buttonInfo.input();
    }

    public int button() {
        return this.buttonInfo.button();
    }

    @Override
    public int modifiers() {
        return this.buttonInfo.modifiers();
    }
}
