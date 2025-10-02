package main.walksy.lib.core.gui.widgets.sub;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.mixin.TextFieldWidgetAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.function.Consumer;

public class TextboxSubWidget extends SubWidget { //TODO Fix selection issue with field width

    private final TextFieldWidget field;
    private final WalksyLibConfigScreen parent;
    public boolean hovered = false;
    private boolean centered;
    private int scrollWidth;

    public TextboxSubWidget(WalksyLibConfigScreen parent, int x, int y, int width, int scrollWidth, int height, String defaultV, Consumer<String> onChange, boolean centered) {
        super(x, y, width, height);
        this.parent = parent;
        this.scrollWidth = scrollWidth;
        this.field = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, x, y, width, height, Text.of(defaultV));
        this.field.setChangedListener(onChange);
        this.field.setText(defaultV);
        this.field.active = true;
        this.centered = centered;
    }

    public int getScrollOffset() {
        var tr = MinecraftClient.getInstance().textRenderer;
        int textWidth = tr.getWidth(field.getText());
        return Math.max(0, textWidth - this.scrollWidth);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!field.isVisible()) return;

        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int color = (hovered || field.isFocused()) ? -1 : new Color(255, 255, 255, 180).getRGB();
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String text = field.getText();
        int scrollOffset = getScrollOffset();

        if (text.isEmpty() && !field.isFocused()) {
            context.drawTextWithShadow(tr, "...", x + 3, y + 1 + tr.fontHeight / 2, color);
        }

        if (field.isFocused() && (parent.tickCount % 20) < 10) {
            var accessor = (TextFieldWidgetAccessor) field;
            int cursor = MathHelper.clamp(field.getCursor() - accessor.getFirstCharacterIndex(), 0, text.length());
            String visible = MinecraftClient.getInstance().textRenderer.trimToWidth(text.substring(accessor.getFirstCharacterIndex()), field.getInnerWidth());
            int caretX = x + 4 + MinecraftClient.getInstance().textRenderer.getWidth(visible.substring(0, MathHelper.clamp(cursor, 0, visible.length()))) - 1 - scrollOffset;
            WalksyLib.get2DRenderer().drawVerticalLine(context, caretX, y + tr.fontHeight / 2 - 1, y + tr.fontHeight + 5, -1);
        }



        if (centered) {
            String trimmed = tr.trimToWidth(text, field.getInnerWidth());
            context.drawTextWithShadow(tr, trimmed, x + width / 2 - tr.getWidth(trimmed) / 2, y + 1 + tr.fontHeight / 2, color);
        } else {
            context.drawTextWithShadow(tr, text, x + 3 - scrollOffset, y + 1 + tr.fontHeight / 2, color);
        }

        int textX = x + 4 - scrollOffset;
        int textY = y + 1 + tr.fontHeight / 2;

        int firstCharIndex = ((TextFieldWidgetAccessor) field).getFirstCharacterIndex();
        String visibleText = tr.trimToWidth(text.substring(firstCharIndex), field.getInnerWidth());

        int selectionStart = MathHelper.clamp(((TextFieldWidgetAccessor) field).getSelectionStart(), 0, text.length());
        int selectionEnd = MathHelper.clamp(((TextFieldWidgetAccessor) field).getSelectionEnd(), 0, text.length());

        int visibleSelectionStart = MathHelper.clamp(selectionStart - firstCharIndex, 0, visibleText.length());
        int visibleSelectionEnd = MathHelper.clamp(selectionEnd - firstCharIndex, 0, visibleText.length());

        if (visibleSelectionStart != visibleSelectionEnd) {
            int highlightStartX = textX + tr.getWidth(visibleText.substring(0, visibleSelectionStart));
            int highlightEndX = textX + tr.getWidth(visibleText.substring(0, visibleSelectionEnd));
            drawSelectionHighlight(context, highlightStartX - 1, textY - 1, highlightEndX - 1, textY + 9);
        }
    }

    @Override
    public void onClick(int mouseX, int mouseY, int button) {
        if (hovered) {
            field.onClick(mouseX, mouseY);
            field.setCursorToEnd(false);
            this.setFocus(true);
            ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
        } else {
            this.setFocus(false);
        }
    }

    @Override
    public void onDrag(int mouseX) {}

    @Override
    public void onKeyPress(int keyCode, int scanCode, int modifiers) {
        field.keyPressed(keyCode, scanCode, modifiers);
        super.onKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public void onCharTyped(char chr, int modifiers) {
        field.charTyped(chr, modifiers);
        super.onCharTyped(chr, modifiers);
    }

    public void setFocus(boolean focus) {
        this.field.setFocused(focus);
    }

    public boolean isFocused() {
        return this.field.isFocused();
    }

    public String getText() {
        return this.field.getText();
    }

    public void setText(String text)
    {
        this.field.setText(text);
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

        if (x2 > x + this.width) x2 = x + this.width;
        if (x1 > x + this.width) x1 = x + this.width;

        context.fill(RenderLayer.getGuiTextHighlight(), x1, y1, x2, y2, -16776961);
    }
}
