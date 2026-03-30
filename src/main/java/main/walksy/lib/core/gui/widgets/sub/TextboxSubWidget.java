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

    public TextboxSubWidget(WalksyLibConfigScreen parent, int x, int y, int width, int scrollWidth, int height, String defaultV, Consumer<String> onChange, boolean centered) {
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
        var tr = Minecraft.getInstance().font;
        int textWidth = tr.width(field.getValue());
        return Math.max(0, textWidth - this.scrollWidth);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!field.isVisible()) return;

        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;


        Font tr = Minecraft.getInstance().font;
        String text = field.getValue();
        int scrollOffset = getScrollOffset();
        int color = (hovered || field.isFocused()) ? -1 : new Color(255, 255, 255, 180).getRGB();

        if (text.isEmpty() && !field.isFocused()) {
            context.text(tr, "...", x + 3, y + (tr.lineHeight) / 2, Color.GRAY.getRGB(), true);
            return;
        }

        var accessor = (EditBoxAccessor) field;

        if (field.isFocused() && (parent.tickCount % 20) < 10) {
            int cursor = Mth.clamp(field.getCursorPosition() - accessor.getDisplayPosition(), 0, text.length());
            String visible = tr.plainSubstrByWidth(text.substring(accessor.getDisplayPosition()), field.getInnerWidth());
            int caretX = x + 4 + tr.width(visible.substring(0, Mth.clamp(cursor, 0, visible.length()))) - 1 - scrollOffset;
            context.verticalLine(caretX, y + tr.lineHeight / 2 - 2, y + tr.lineHeight + 4, -1);
        }

        if (centered) {
            String trimmed = tr.plainSubstrByWidth(text, field.getInnerWidth());
            context.text(tr, trimmed, x + width / 2 - tr.width(trimmed) / 2, y + 1 + tr.lineHeight / 2, color);
        } else {
            context.text(tr, text, x + 3 - scrollOffset, y + 1 + tr.lineHeight / 2, color, true);
        }

        int textX = x + 4 - scrollOffset;
        int textY = y + 1 + tr.lineHeight / 2;

        int firstCharIndex = accessor.getDisplayPosition();
        String visibleText = tr.plainSubstrByWidth(text.substring(firstCharIndex), field.getInnerWidth());

        int selectionStart = Mth.clamp(this.getSelectionStart(accessor), 0, text.length());
        int selectionEnd = Mth.clamp(this.getSelectionEnd(accessor), 0, text.length());

        int visibleSelectionStart = Mth.clamp(selectionStart - firstCharIndex, 0, visibleText.length());
        int visibleSelectionEnd = Mth.clamp(selectionEnd - firstCharIndex, 0, visibleText.length());

        if (visibleSelectionStart != visibleSelectionEnd) {
            int highlightStartX = textX + tr.width(visibleText.substring(0, visibleSelectionStart));
            int highlightEndX = textX + tr.width(visibleText.substring(0, visibleSelectionEnd));
            drawSelectionHighlight(context, highlightStartX - 1, textY - 1, highlightEndX - 1, textY + 9);
        }
    }

    private int getSelectionStart(EditBoxAccessor editBox) {
        return Math.min(this.field.getCursorPosition(), editBox.getHighlightPos());
    }

    private int getSelectionEnd(EditBoxAccessor editBox) {
        return Math.max(this.field.getCursorPosition(), editBox.getHighlightPos());
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        if (hovered) {
            field.onClick(click, doubled);
            field.moveCursorToEnd(false);
            this.setFocus(true);
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
        } else {
            this.setFocus(false);
        }
    }

    @Override
    public void onDrag(int mouseX) {}

    @Override
    public void onKeyPress(KeyEvent input) {
        field.keyPressed(input);
        super.onKeyPress(input);
    }

    @Override
    public void onCharTyped(CharacterEvent input) {
        field.charTyped(input);
        super.onCharTyped(input);
    }

    public void setOnFocusLost(Runnable onFocusLost) {
        this.onFocusLost = onFocusLost;
    }

    public void setFocus(boolean focus) {
        this.field.setFocused(focus);
        if (!focus && onFocusLost != null) {
            onFocusLost.run();
        }
    }

    public boolean isFocused() {
        return this.field.isFocused();
    }

    public String getText() {
        return this.field.getValue();
    }

    public void setText(String text)
    {
        this.field.setValue(text);
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

        if (x2 > x + this.width) x2 = x + this.width;
        if (x1 > x + this.width) x1 = x + this.width;

        context.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, x1, y1, x2, y2, -16776961);
    }
}
