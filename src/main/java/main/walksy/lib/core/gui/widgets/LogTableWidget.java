package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.MarqueeUtil;
import main.walksy.lib.core.utils.Scroller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LogTableWidget extends AbstractWidget {

    private final Screen parent;
    private final List<String> entries = new ArrayList<>();
    private final List<List<String>> rowEntries = new ArrayList<>();
    private final Scroller scroller = new Scroller(0, 12);

    public LogTableWidget(String name, Screen parent, int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(name));
        this.parent = parent;
    }

    public void addEntryGroup(String... lines) {
        Collections.addAll(entries, lines);
    }

    public void addLogRow(String... entries) {
        rowEntries.add(Arrays.asList(entries));
        int visibleRows = (height - 40) / 12;
        int totalRows = rowEntries.size();
        int scrollMax = Math.max(0, (totalRows - visibleRows) * 12);
        scroller.setBounds(0, scrollMax);
    }


    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int tableOffset = 10;
        WalksyLib.get2DRenderer().fillRoundedRect(
                context,
                getX(),
                getY() + tableOffset,
                width,
                height,
                2,
                new Color(0, 0, 0, 100).getRGB()
        );
        WalksyLib.get2DRenderer().fillRoundedRectOutline(
                context,
                getX() + 1,
                getY() + 1 + tableOffset,
                width - 2,
                height - 2,
                2,
                1,
                MainColors.OUTLINE_WHITE.getRGB()
        );
        WalksyLib.get2DRenderer().fillRoundedRectOutline(
                context,
                getX(),
                getY() + tableOffset,
                width,
                height,
                2,
                1,
                MainColors.OUTLINE_BLACK.getRGB()
        );
        context.drawHorizontalLine(getX() + 2, getX() + width - 3, getY() + 32, MainColors.OUTLINE_WHITE.getRGB());

        hovered = mouseX >= getX() &&
                mouseX <= getX() + width &&
                mouseY >= getY() + 33 &&
                mouseY <= getY() + height + 8;

        context.enableScissor(getX(), getY() + 33, getX() + width, getY() + height + 8);

        if (!rowEntries.isEmpty()) {
            int colWidth = (width - 12) / rowEntries.get(0).size();
            int xStart = getX() + 6;
            int yTop = getY() + tableOffset + 30;
            int rowHeight = 12;
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            for (int row = 0; row < rowEntries.size(); row++) {
                List<String> columns = rowEntries.get(row);
                int y = yTop + row * rowHeight - (int) scroller.getValue();

                for (int col = 0; col < columns.size(); col++) {
                    String rawEntry = columns.get(col);

                    String entry = MarqueeUtil.get(rawEntry, colWidth - 10, 10);

                    int textWidth = textRenderer.getWidth(entry);
                    int xPos = xStart + (colWidth * col) + (colWidth - textWidth) / 2;

                    context.drawText(textRenderer, entry, xPos, y, Color.LIGHT_GRAY.getRGB(), false);
                }
            }
        }

        context.disableScissor();

        if (!entries.isEmpty()) {
            int colWidth = (width - 12) / entries.size();
            int xStart = getX() + 6;
            int yTop = getY() + tableOffset + 3;
            int yBottom = getY() + 35;

            for (int i = 0; i < entries.size(); i++) {
                String entry = entries.get(i);
                int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(entry);
                int xPos = xStart + (colWidth * i) + (colWidth - textWidth) / 2;

                context.drawText(MinecraftClient.getInstance().textRenderer, entry, xPos, yTop + 6, Color.LIGHT_GRAY.getRGB(), false);

                if (i < entries.size() - 1) {
                    int lineX = xStart + colWidth * (i + 1);
                    context.drawVerticalLine(lineX, yTop - 2, yBottom - 3, MainColors.OUTLINE_WHITE.getRGB());
                }
            }
        }




        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getMessage(), parent.width / 2, 58, -1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHovered()) {
            scroller.onScroll(verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }


    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
