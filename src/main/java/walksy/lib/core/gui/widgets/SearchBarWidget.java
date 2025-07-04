package walksy.lib.core.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import walksy.lib.core.mixin.TextFieldWidgetAccessor;
import walksy.lib.core.utils.MainColors;
import walksy.lib.core.utils.RenderUtils;

import java.awt.*;
import java.util.Objects;

public class SearchBarWidget extends TextFieldWidget {

    private final WalksyLibConfigScreen parent;

    public SearchBarWidget(int x, int y, int width, int height, Text text, WalksyLibConfigScreen parent) {
        super(MinecraftClient.getInstance().textRenderer, x, y, width, height, text);
        this.parent = parent;
        this.setMaxLength((width - 8) / 6);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.isVisible()) {
            context.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2, MainColors.OUTLINE_BLACK.getRGB());

            //fuckass fix for my shitty filledroundedrec code
            //TOP AND BOTTOM
            context.fill(getX() + 3, getY() + 1, getX() + getWidth() - 3, getY() + 2, MainColors.OUTLINE_BLACK.getRGB());
            context.fill(getX() + 3, getY() + getHeight() - 2, getX() + getWidth() - 3, getY() + getHeight() - 1, MainColors.OUTLINE_BLACK.getRGB());
            //LEFT AND RIGHT
            context.fill(getX() + 1, getY() + 3, getX() + 2, getY() + getHeight() - 3, MainColors.OUTLINE_BLACK.getRGB());
            context.fill(getX() + getWidth() - 1, getY() + 3, getX() + getWidth() - 2, getY() + getHeight() - 3, MainColors.OUTLINE_BLACK.getRGB());

            int color = hovered ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
            if (this.isFocused()) {
                color = Color.WHITE.getRGB();
            }
            RenderUtils.fillRoundedRectOutline(context, getX(), getY(), getWidth(), getHeight(), 2, 1, color);

            if (this.getText().isEmpty() && !this.isFocused()) {
                context.drawTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    "Search...",
                    getX() + 3,
                    getY() + (MinecraftClient.getInstance().textRenderer.fontHeight) / 2,
                    Color.GRAY.getRGB()
                );

            }


            if (this.isFocused() && (parent.tickCount % 20) < 10) {
                context.drawVerticalLine(
                    (getX() + 4) + MinecraftClient.getInstance().textRenderer.getWidth(this.getText()),
                    getY() + ((MinecraftClient.getInstance().textRenderer.fontHeight) / 2) - 2,
                    getY() + ((MinecraftClient.getInstance().textRenderer.fontHeight)) + 4,
                    -1
                );
            }

            context.drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                this.getText(),
                getX() + 3,
                getY() + (MinecraftClient.getInstance().textRenderer.fontHeight) / 2,
                -1
            );

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int textX = getX() + 4;
            int textY = getY() + (this.height - 8) / 2;

            String visibleText = textRenderer.trimToWidth(this.getText().substring(((TextFieldWidgetAccessor) this).getFirstCharacterIndex()), this.getInnerWidth());


            int selectionStart = MathHelper.clamp(((TextFieldWidgetAccessor) this).getSelectionStart(), 0, this.getText().length());
            int selectionEnd = MathHelper.clamp(((TextFieldWidgetAccessor) this).getSelectionEnd(), 0, this.getText().length());
            int firstCharIndex = ((TextFieldWidgetAccessor) this).getFirstCharacterIndex();

            int visibleSelectionStart = MathHelper.clamp(selectionStart - firstCharIndex, 0, visibleText.length());
            int visibleSelectionEnd = MathHelper.clamp(selectionEnd - firstCharIndex, 0, visibleText.length());

            if (visibleSelectionStart != visibleSelectionEnd) {
                int highlightStartX = textX + textRenderer.getWidth(visibleText.substring(0, visibleSelectionStart));
                int highlightEndX = textX + textRenderer.getWidth(visibleText.substring(0, visibleSelectionEnd));
                int highlightTop = textY - 1;
                int highlightBottom = textY + 9;

                drawSelectionHighlight(context, highlightStartX - 1, highlightTop, highlightEndX - 1, highlightBottom);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);

    }

    private void drawSelectionHighlight(DrawContext context, int x1, int y1, int x2, int y2) {
        if (x1 < x2) {
            int i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            int i = y1;
            y1 = y2;
            y2 = i;
        }

        if (x2 > this.getX() + this.width) {
            x2 = this.getX() + this.width;
        }

        if (x1 > this.getX() + this.width) {
            x1 = this.getX() + this.width;
        }

        context.fill(RenderLayer.getGuiTextHighlight(), x1, y1, x2, y2, -16776961);
    }
}
