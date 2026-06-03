package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.mixin.EditBoxAccessor;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.function.Consumer;

public class SearchBarWidget extends EditBox {

    private final WalksyLibConfigScreen parent;
    private final Consumer<String> searchQuery;

    public SearchBarWidget(final WalksyLibConfigScreen parent, final int x, final int y, final int width, final int height, final Consumer<String> searchQuery) {
        super(Minecraft.getInstance().font, x, y, width, height, net.minecraft.network.chat.Component.empty());
        this.parent = parent;
        this.searchQuery = searchQuery;
        this.setMaxLength((width - 8) / 6);
    }

    private int getFirstCharacterIndex() {
        return ((EditBoxAccessor) this).getDisplayPosition();
    }

    private int getSelectionStart() {
        return Math.min(this.getCursorPosition(), ((EditBoxAccessor) this).getHighlightPos());
    }

    private int getSelectionEnd() {
        return Math.max(this.getCursorPosition(), ((EditBoxAccessor) this).getHighlightPos());
    }

    @Override
    public void extractWidgetRenderState(final GuiGraphicsExtractor extractor, final int mouseX, final int mouseY, final float a) {
        if (this.isVisible()) {
            final Graphics g = this.parent.currentGraphicsContext();
            g.fillRoundedRect(this.getX() + 1, this.getY() + 1, this.getWidth() - 2, this.getHeight() - 2, 2, MainColors.OUTLINE_BLACK.getRGB());

            int color = this.isHovered ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
            if (this.isFocused()) {
                color = Color.WHITE.getRGB();
            }
            g.fillRoundedRectOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 2, 1, color);

            if (this.getValue().isEmpty() && !this.isFocused()) {
                extractor.text(
                        this.parent.getFont(),
                        "Search...",
                        this.getX() + 3,
                        this.getY() + (this.parent.getFont().lineHeight) / 2,
                        Color.GRAY.getRGB(),
                        true
                );
            }

            if (this.isFocused() && (this.parent.getUpTime() % 20) < 10) {
                extractor.verticalLine(
                        (this.getX() + 4) + this.parent.getFont().width(this.getValue()),
                        this.getY() + ((this.parent.getFont().lineHeight) / 2) - 2,
                        this.getY() + ((this.parent.getFont().lineHeight)) + 4,
                        -1
                );
            }

            extractor.text(
                    this.parent.getFont(),
                    this.getValue(),
                    this.getX() + 3,
                    this.getY() + (this.parent.getFont().lineHeight) / 2,
                    -1,
                    true
            );

            final Font textRenderer = this.parent.getFont();
            final int textX = this.getX() + 4;
            final int textY = this.getY() + (this.height - 8) / 2;

            final int firstCharIndex = this.getFirstCharacterIndex();
            final String visibleText = textRenderer.plainSubstrByWidth(this.getValue().substring(firstCharIndex), this.getInnerWidth());

            final int selectionStart = Mth.clamp(this.getSelectionStart(), 0, this.getValue().length());
            final int selectionEnd = Mth.clamp(this.getSelectionEnd(), 0, this.getValue().length());

            final int visibleSelectionStart = Mth.clamp(selectionStart - firstCharIndex, 0, visibleText.length());
            final int visibleSelectionEnd = Mth.clamp(selectionEnd - firstCharIndex, 0, visibleText.length());

            if (visibleSelectionStart != visibleSelectionEnd) {
                final int highlightStartX = textX + textRenderer.width(visibleText.substring(0, visibleSelectionStart));
                final int highlightEndX = textX + textRenderer.width(visibleText.substring(0, visibleSelectionEnd));
                final int highlightTop = textY - 1;
                final int highlightBottom = textY + 9;

                this.drawSelectionHighlight(extractor, highlightStartX - 1, highlightTop, highlightEndX - 1, highlightBottom);
            }
        }
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.isHovered()) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
        }
        super.onClick(event, doubleClick);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final boolean result = super.keyPressed(event);
        this.searchQuery.accept(this.getValue().toLowerCase());
        return result;
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        final boolean result = super.charTyped(event);
        this.searchQuery.accept(this.getValue().toLowerCase());
        return result;
    }

    private void drawSelectionHighlight(final GuiGraphicsExtractor extractor, int x1, int y1, int x2, int y2) {
        if (x1 < x2) {
            final int i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2) {
            final int i = y1;
            y1 = y2;
            y2 = i;
        }

        if (x2 > this.getX() + this.width) x2 = this.getX() + this.width;
        if (x1 > this.getX() + this.width) x1 = this.getX() + this.width;

        extractor.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, x1, y1, x2, y2, -16776961);
    }
}
