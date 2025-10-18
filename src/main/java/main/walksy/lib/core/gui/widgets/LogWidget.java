package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.MarqueeUtil;
import main.walksy.lib.core.utils.Scroller;
import main.walksy.lib.core.utils.log.InternalLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LogWidget extends AbstractWidget {

    private final Screen parent;
    private final List<InternalLog> logLines = new ArrayList<>();
    private final Scroller scroller = new Scroller(0, 12);

    public LogWidget(String name, Screen parent, int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(name));
        this.parent = parent;
    }

    public void clearLogs()
    {
        logLines.clear();
    }

    public void addLog(InternalLog line) {
        logLines.add(line);
        int visibleRows = (height - 20) / 12;
        int totalRows = logLines.size();
        int scrollMax = Math.max(0, (totalRows - visibleRows) * 12);
        scroller.setBounds(0, scrollMax);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int offset = 10;

        Renderer2D.fillRoundedRect(
                context, getX(), getY() + offset, width, height, 2,
                new Color(0, 0, 0, 100).getRGB()
        );
        Renderer2D.fillRoundedRectOutline(
                context, getX(), getY() + offset, width, height, 2, 1,
                MainColors.OUTLINE_BLACK.getRGB()
        );
        Renderer2D.fillRoundedRectOutline(
                context, getX() + 1, getY() + 1 + offset, width - 2, height - 2, 2, 1,
                MainColors.OUTLINE_WHITE.getRGB()
        );

        hovered = mouseX >= getX() &&
                mouseX <= getX() + width &&
                mouseY >= getY() + offset &&
                mouseY <= getY() + height;

        context.enableScissor(getX(), getY() + offset, getX() + width, getY() + height);

        if (!logLines.isEmpty()) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int rowHeight = 12;
            int yTop = getY() + offset + 5;

            for (int i = 0; i < logLines.size(); i++) {
                int y = yTop + i * rowHeight - (int) scroller.getValue();

                if (y + rowHeight < getY() + offset || y > getY() + height) continue;

                InternalLog.ToolTip logToolTip = logLines.get(i).getToolTip();
                int c = Color.LIGHT_GRAY.getRGB();

                if (logToolTip != null) {
                    c = logToolTip.color();
                    this.setTooltip(isHoveredLog(mouseX, mouseY) ? logToolTip.tooltip() : null);
                } else {
                    this.setTooltip(null);
                }

                String entry = MarqueeUtil.get(logLines.get(i).getText(), width - 10, 10);
                context.drawText(textRenderer, entry, getX() + 6, y, c, false);
            }
        }

        context.disableScissor();

        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getMessage(),
                parent.width / 2,
                getY() - 4,
                -1
        );
    }

    private boolean isHoveredLog(double mouseX, double mouseY) {
        if (logLines.isEmpty()) return false;

        int rowHeight = 12;
        int offset = 10;
        int yTop = getY() + offset + 5;
        if (mouseX < getX() || mouseX > getX() + width ||
                mouseY < getY() + offset || mouseY > getY() + height) {
            return false;
        }

        int index = (int) ((mouseY - yTop + scroller.getValue()) / rowHeight);

        return index >= 0 && index < logLines.size();
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHovered()) {
            scroller.onScroll(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
