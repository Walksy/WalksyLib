package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.mixin.EditBoxAccessor;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.function.Consumer;

public class SearchBarWidget extends EditBox {

    private final WalksyLibConfigScreen parent;
    private final Consumer<String> searchQuery;

    public SearchBarWidget(WalksyLibConfigScreen parent, int x, int y, int width, int height, Consumer<String> searchQuery) {
        super(Minecraft.getInstance().font, x, y, width, height, net.minecraft.network.chat.Component.empty());
        this.parent = parent;
        this.searchQuery = searchQuery;
        this.setMaxLength((width - 8) / 6);
    }

    private int getFirstCharacterIndex() {
        return ((EditBoxAccessor)this).getDisplayPosition();
    }

    private int getSelectionStart() {
        return Math.min(getCursorPosition(), getHighlightPos());
    }

    private int getSelectionEnd() {
        return Math.max(getCursorPosition(), getHighlightPos());
    }

    private int getHighlightPos() {
        return ((EditBoxAccessor)this).getHighlightPos();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        if (this.isVisible()) {

            Renderer2D.fillRoundedRect(context, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 2, MainColors.OUTLINE_BLACK.getRGB());

            int color = isHovered ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
            if (this.isFocused()) {
                color = Color.WHITE.getRGB();
            }
            Renderer2D.fillRoundedRectOutline(context, getX(), getY(), getWidth(), getHeight(), 2, 1, color);

            if (this.getValue().isEmpty() && !this.isFocused()) {
                context.text(
                        this.parent.getFont(),
                        "Search...",
                        getX() + 3,
                        getY() + (this.parent.getFont().lineHeight) / 2,
                        Color.GRAY.getRGB(),
                        true
                );
            }

            if (this.isFocused() && (parent.tickCount % 20) < 10) {
                context.verticalLine(
                        (getX() + 4) + this.parent.getFont().width(this.getValue()),
                        getY() + ((this.parent.getFont().lineHeight) / 2) - 2,
                        getY() + ((this.parent.getFont().lineHeight)) + 4,
                        -1
                );
            }

            context.text(
                    this.parent.getFont(),
                    this.getValue(),
                    getX() + 3,
                    getY() + (this.parent.getFont().lineHeight) / 2,
                    -1,
                    true
            );

            Font textRenderer = this.parent.getFont();
            int textX = getX() + 4;
            int textY = getY() + (this.height - 8) / 2;

            int firstCharIndex = getFirstCharacterIndex();
            String visibleText = textRenderer.plainSubstrByWidth(this.getValue().substring(firstCharIndex), this.getInnerWidth());

            int selectionStart = Mth.clamp(getSelectionStart(), 0, this.getValue().length());
            int selectionEnd = Mth.clamp(getSelectionEnd(), 0, this.getValue().length());

            int visibleSelectionStart = Mth.clamp(selectionStart - firstCharIndex, 0, visibleText.length());
            int visibleSelectionEnd = Mth.clamp(selectionEnd - firstCharIndex, 0, visibleText.length());

            if (visibleSelectionStart != visibleSelectionEnd) {
                int highlightStartX = textX + textRenderer.width(visibleText.substring(0, visibleSelectionStart));
                int highlightEndX = textX + textRenderer.width(visibleText.substring(0, visibleSelectionEnd));
                int highlightTop = textY - 1;
                int highlightBottom = textY + 9;

                drawSelectionHighlight(context, highlightStartX - 1, highlightTop, highlightEndX - 1, highlightBottom);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean result = super.keyPressed(event);
        searchQuery.accept(this.getValue().toLowerCase());
        return result;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        boolean result = super.charTyped(event);
        searchQuery.accept(this.getValue().toLowerCase());
        return result;
    }


    private void drawSelectionHighlight(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2) {
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

        if (x2 > this.getX() + this.width) x2 = this.getX() + this.width;
        if (x1 > this.getX() + this.width) x1 = this.getX() + this.width;

        context.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, x1, y1, x2, y2, -16776961);
    }
}