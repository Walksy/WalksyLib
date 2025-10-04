package main.walksy.lib.core.utils.log;

import net.minecraft.client.gui.tooltip.Tooltip;

public class InternalLog {
    private final String text;
    private final ToolTip toolTip;

    private InternalLog(String text)
    {
        this(text, null);
    }

    private InternalLog(String text, ToolTip toolTip)
    {
        this.text = text;
        this.toolTip = toolTip;
    }

    public static InternalLog of(String text)
    {
        return of(text, null);
    }

    public static InternalLog of(String text, ToolTip toolTip)
    {
        return new InternalLog(text, toolTip);
    }

    public String getText()
    {
        return text;
    }

    public ToolTip getToolTip()
    {
        return this.toolTip;
    }

    public record ToolTip(Tooltip tooltip, int color) {}
}
