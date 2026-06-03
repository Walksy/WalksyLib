package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.MarqueeUtil;
import main.walksy.lib.core.utils.Scroller;
import main.walksy.lib.core.utils.log.InternalLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LogWidget extends AbstractWidget {

    private final Screen parent;
    private final List<InternalLog> logLines = new ArrayList<>();
    private final Scroller scroller = new Scroller(0, 12);

    public LogWidget(final String name, final Screen parent, final int x, final int y, final int width, final int height) {
        super(x, y, width, height, Component.literal(name));
        this.parent = parent;
    }

    public void clearLogs() {
        this.logLines.clear();
    }

    public void addLog(final InternalLog line) {
        this.logLines.add(line);
        final int visibleRows = (this.height - 20) / 12;
        final int totalRows = this.logLines.size();
        final int scrollMax = Math.max(0, (totalRows - visibleRows) * 12);
        this.scroller.setBounds(0, scrollMax);
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor extractor, final int mouseX, final int mouseY, final float a) {
        final int offset = 10;
        final Graphics graphics = new Graphics(extractor);

        graphics.fillRoundedRect(
                this.getX(), this.getY() + offset, this.width, this.height, 2,
                new Color(0, 0, 0, 100).getRGB()
        );
        graphics.fillRoundedRectOutline(
                this.getX(), this.getY() + offset, this.width, this.height, 2, 1,
                MainColors.OUTLINE_BLACK.getRGB()
        );
        graphics.fillRoundedRectOutline(
                this.getX() + 1, this.getY() + 1 + offset, this.width - 2, this.height - 2, 2, 1,
                MainColors.OUTLINE_WHITE.getRGB()
        );

        this.isHovered = mouseX >= this.getX() &&
                mouseX <= this.getX() + this.width &&
                mouseY >= this.getY() + offset &&
                mouseY <= this.getY() + this.height;

        extractor.enableScissor(this.getX(), this.getY() + offset + 2, this.getX() + this.width, this.getY() + this.height + 5);

        if (!this.logLines.isEmpty()) {
            final Font textRenderer = Minecraft.getInstance().font;
            final int rowHeight = 12;
            final int yTop = this.getY() + offset + 5;

            for (int i = 0; i < this.logLines.size(); i++) {
                final int y = yTop + i * rowHeight - (int) this.scroller.getValue();

                if (y + rowHeight < this.getY() + offset || y > this.getY() + this.height) continue;

                final InternalLog.ToolTip logToolTip = this.logLines.get(i).getToolTip();
                int c = Color.LIGHT_GRAY.getRGB();

                if (logToolTip != null) {
                    c = logToolTip.color();
                    this.setTooltip(this.isHoveredLog(mouseX, mouseY) ? logToolTip.tooltip() : null);
                } else {
                    this.setTooltip(null);
                }

                final String entry = MarqueeUtil.get(this.logLines.get(i).getText(), this.width - 10, 10);
                extractor.text(textRenderer, entry, this.getX() + 6, y, c, false);
            }
        }

        extractor.disableScissor();

        extractor.centeredText(
                Minecraft.getInstance().font,
                this.getMessage(),
                this.parent.width / 2,
                this.getY() - 4,
                -1
        );
    }

    private boolean isHoveredLog(final double mouseX, final double mouseY) {
        if (this.logLines.isEmpty()) return false;
        final int rowHeight = 12;
        final int offset = 10;
        final int yTop = this.getY() + offset + 5;
        if (mouseX < this.getX() || mouseX > this.getX() + this.width ||
                mouseY < this.getY() + offset || mouseY > this.getY() + this.height) {
            return false;
        }

        final int index = (int) ((mouseY - yTop + this.scroller.getValue()) / rowHeight);

        return index >= 0 && index < this.logLines.size();
    }


    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double horizontalAmount, final double verticalAmount) {
        if (this.isHovered()) {
            this.scroller.onScroll(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {}
}
