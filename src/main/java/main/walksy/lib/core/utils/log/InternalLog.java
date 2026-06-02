package main.walksy.lib.core.utils.log;

import net.minecraft.client.gui.components.Tooltip;

public class InternalLog {
    private final String text;
    private final ToolTip toolTip;

    private InternalLog(final String text) {
        this(text, null);
    }

    private InternalLog(final String text, final ToolTip toolTip) {
        this.text = text;
        this.toolTip = toolTip;
    }

    public static InternalLog of(final String text) {
        return of(text, null);
    }

    public static InternalLog of(final String text, final ToolTip toolTip) {
        return new InternalLog(text, toolTip);
    }

    public String getText() {
        return this.text;
    }

    public ToolTip getToolTip() {
        return this.toolTip;
    }

    public record ToolTip(Tooltip tooltip, int color) {}
}
