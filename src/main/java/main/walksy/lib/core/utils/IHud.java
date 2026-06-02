package main.walksy.lib.core.utils;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface IHud {
    void extractCrosshair$walksyLib(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker);
}
