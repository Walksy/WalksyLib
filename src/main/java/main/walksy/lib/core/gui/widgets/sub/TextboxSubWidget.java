package main.walksy.lib.core.gui.widgets.sub;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.mixin.EditBoxAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.function.Consumer;

public class TextboxSubWidget extends SubWidget {

    private final EditBox field;
    private final WalksyLibConfigScreen parent;
    public boolean hovered = false;
    private boolean centered;
    private int scrollWidth;
    private Runnable onFocusLost = null;

    public TextboxSubWidget(final WalksyLibConfigScreen parent, final int x, final int y, final int width, final int scrollWidth, final int height, final String defaultV, final Consumer<String> onChange, final boolean centered) {
        super(x, y, width, height);
        this.parent = parent;
        this.scrollWidth = scrollWidth;
        this.field = new EditBox(Minecraft.getInstance().font, x, y, width, height, Component.literal(defaultV));
        this.field.setResponder(onChange);
        this.field.setValue(defaultV);
        this.field.active = true;
        this.centered = centered;
    }

    public int getScrollOffset() {
        final var tr = Minecraft.getInstance().font;
        final int textWidth = tr.width(this.field.getValue());
        return Math.max(0, textWidth - this.scrollWidth);
    }

    @Override
    public void render(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {
        if (!this.field.isVisible()) return;

        this.hovered = mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;

        final Font tr = Minecraft.getInstance().font;
        final String text = this.field.getValue();
        final int scrollOffset = this.getScrollOffset();
        final int color = (this.hovered || this.field.isFocused()) ? -1 : new Color(255, 255, 255, 180).getRGB();

        if (text.isEmpty() && !this.field.isFocused()) {
            context.text(tr, "...", this.x + 3, this.y + (tr.lineHeight) / 2, Color.GRAY.getRGB(), true);
            return;
        }

        final var accessor = (EditBoxAccessor) this.field;

        if (this.field.isFocused() && (this.parent.upTime % 20) < 10) {
            final int cursor = Mth.clamp(this.field.getCursorPosition() - accessor.getDisplayPosition(), 0, text.length());
            final String visible = tr.plainSubstrByWidth(text.substring(accessor.getDisplayPosition()), this.field.getInnerWidth());
            final int caretX = this.x + 4 + tr.width(visible.substring(0, Mth.clamp(cursor, 0, visible.length()))) - 1 - scrollOffset;
            context.verticalLine(caretX, this.y + tr.lineHeight / 2 - 2, this.y + tr.lineHeight + 4, -1);
        }

        if (this.centered) {
            final String trimmed = tr.plainSubstrByWidth(text, this.field.getInnerWidth());
            context.text(tr, trimmed, this.x + this.width / 2 - tr.width(trimmed) / 2, this.y + 1 + tr.lineHeight / 2, color);
        } else {
            context.text(tr, text, this.x + 3 - scrollOffset, this.y + 1 + tr.lineHeight / 2, color, true);
        }

        final int textX = this.x + 4 - scrollOffset;
        final int textY = this.y + 1 + tr.lineHeight / 2;

        final int firstCharIndex = accessor.getDisplayPosition();
        final String visibleText = tr.plainSubstrByWidth(text.substring(firstCharIndex), this.field.getInnerWidth());

        final int selectionStart = Mth.clamp(this.getSelectionStart(accessor), 0, text.length());
        final int selectionEnd = Mth.clamp(this.getSelectionEnd(accessor), 0, text.length());

        final int visibleSelectionStart = Mth.clamp(selectionStart - firstCharIndex, 0, visibleText.length());
        final int visibleSelectionEnd = Mth.clamp(selectionEnd - firstCharIndex, 0, visibleText.length());

        if (visibleSelectionStart != visibleSelectionEnd) {
            final int highlightStartX = textX + tr.width(visibleText.substring(0, visibleSelectionStart));
            final int highlightEndX = textX + tr.width(visibleText.substring(0, visibleSelectionEnd));
            this.drawSelectionHighlight(context, highlightStartX - 1, textY - 1, highlightEndX - 1, textY + 9);
        }
    }

    private int getSelectionStart(final EditBoxAccessor editBox) {
        return Math.min(this.field.getCursorPosition(), editBox.getHighlightPos());
    }

    private int getSelectionEnd(final EditBoxAccessor editBox) {
        return Math.max(this.field.getCursorPosition(), editBox.getHighlightPos());
    }

    @Override
    public void onClick(final MouseButtonEvent click, final boolean doubled) {
        if (this.hovered) {
            this.field.onClick(click, doubled);
            this.field.moveCursorToEnd(false);
            this.setFocus(true);
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
        } else {
            this.setFocus(false);
        }
    }

    @Override
    public void onDrag(final int mouseX) {}

    @Override
    public void onKeyPress(final KeyEvent input) {
        this.field.keyPressed(input);
        super.onKeyPress(input);
    }

    @Override
    public void onCharTyped(final CharacterEvent input) {
        this.field.charTyped(input);
        super.onCharTyped(input);
    }

    public void setOnFocusLost(final Runnable onFocusLost) {
        this.onFocusLost = onFocusLost;
    }

    public void setFocus(final boolean focus) {
        this.field.setFocused(focus);
        if (!focus && this.onFocusLost != null) {
            this.onFocusLost.run();
        }
    }

    public boolean isFocused() {
        return this.field.isFocused();
    }

    public String getText() {
        return this.field.getValue();
    }

    public void setText(final String text) {
        this.field.setValue(text);
    }

    private void drawSelectionHighlight(final GuiGraphicsExtractor context, int x1, int y1, int x2, int y2) {
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

        if (x2 > this.x + this.width) x2 = this.x + this.width;
        if (x1 > this.x + this.width) x1 = this.x + this.width;

        context.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, x1, y1, x2, y2, -16776961);
    }
}
