package main.walksy.lib.core.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.platform.WLInput;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

public final class MultilineTextEditor {
    private static final float TEXT_INSET = 6f;
    private static final float LINE_SPACING = 1.35f;
    private static final float CARET_WIDTH_RATIO = 0.09f;
    private static final float BLINK_PERIOD = 1.0f;
    private static final float SCROLLBAR_W = 2.5f;
    private static final float SCROLLBAR_INSET = 3f;
    private static final float SCROLL_GLIDE_SPEED = 18f;
    private static final String TAB = "  ";
    private final StringBuilder text = new StringBuilder();
    private int caret;
    private int selectionAnchor;
    private float scrollX;
    private float scrollY;
    private float scrollTarget;
    private final ScrollGlide glide = new ScrollGlide();
    private int followedCaret = -1;
    private int followedLength = -1;
    private float caretTimer;
    private boolean focused;
    private float x;
    private float y;
    private float w;
    private float h;
    private float textSize = 8f;
    private float uiScale = 1f;
    private List<String> lines = List.of("");
    private boolean linesStale = true;
    private boolean draggingThumb;
    private float thumbGrabOffset;

    public MultilineTextEditor(final String initial) {
        this.setText(initial);
    }

    public String getText() {
        return this.text.toString();
    }

    public void setText(final String value) {
        this.text.setLength(0);
        if (value != null) {
            this.text.append(value.replace("\r\n", "\n").replace('\r', '\n'));
        }
        this.caret = 0;
        this.selectionAnchor = 0;
        this.scrollX = 0f;
        this.scrollY = 0f;
        this.scrollTarget = 0f;
        this.followedCaret = -1;
        this.linesStale = true;
    }

    private List<String> lines() {
        if (this.linesStale) {
            final List<String> split = new ArrayList<>();
            int start = 0;
            final String all = this.text.toString();
            for (int i = 0; i < all.length(); i++) {
                if (all.charAt(i) == '\n') {
                    split.add(all.substring(start, i));
                    start = i + 1;
                }
            }
            split.add(all.substring(start));
            this.lines = split;
            this.linesStale = false;
        }
        return this.lines;
    }

    private int lineStart(final int line) {
        int index = 0;
        final List<String> all = this.lines();
        for (int i = 0; i < line && i < all.size(); i++) {
            index += all.get(i).length() + 1;
        }
        return index;
    }

    private int lineOf(final int index) {
        final List<String> all = this.lines();
        int counted = 0;
        for (int i = 0; i < all.size(); i++) {
            final int end = counted + all.get(i).length();
            if (index <= end) {
                return i;
            }
            counted = end + 1;
        }
        return all.size() - 1;
    }

    private int columnOf(final int index) {
        return index - this.lineStart(this.lineOf(index));
    }

    private boolean hasSelection() {
        return this.caret != this.selectionAnchor;
    }

    private int selStart() {
        return Math.min(this.caret, this.selectionAnchor);
    }

    private int selEnd() {
        return Math.max(this.caret, this.selectionAnchor);
    }

    private void deleteSelection() {
        if (!this.hasSelection()) {
            return;
        }
        final int start = this.selStart();
        this.text.delete(start, this.selEnd());
        this.caret = start;
        this.selectionAnchor = start;
        this.linesStale = true;
    }

    public void insert(final String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        final String value = sanitize(raw);
        if (value.isEmpty()) {
            return;
        }
        this.deleteSelection();
        this.text.insert(this.caret, value);
        this.caret += value.length();
        this.selectionAnchor = this.caret;
        this.caretTimer = 0f;
        this.linesStale = true;
    }

    private static String sanitize(final String raw) {
        final StringBuilder out = new StringBuilder(raw.length());
        raw.replace("\r\n", "\n").replace('\r', '\n').codePoints().forEach(cp -> {
            if (cp == '\n' || (cp >= 32 && cp != 127)) {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }

    private void backspace() {
        if (this.hasSelection()) {
            this.deleteSelection();
            return;
        }
        if (this.caret > 0) {
            this.text.deleteCharAt(this.caret - 1);
            this.caret--;
            this.selectionAnchor = this.caret;
            this.linesStale = true;
        }
    }

    private void deleteForward() {
        if (this.hasSelection()) {
            this.deleteSelection();
            return;
        }
        if (this.caret < this.text.length()) {
            this.text.deleteCharAt(this.caret);
            this.linesStale = true;
        }
    }

    private void moveCaret(final int to, final boolean select) {
        this.caret = Math.max(0, Math.min(this.text.length(), to));
        if (!select) {
            this.selectionAnchor = this.caret;
        }
        this.caretTimer = 0f;
    }

    private void moveLine(final int delta, final boolean select) {
        final int line = this.lineOf(this.caret);
        final int target = Math.max(0, Math.min(this.lines().size() - 1, line + delta));
        if (target == line) {
            this.moveCaret(delta < 0 ? 0 : this.text.length(), select);
            return;
        }
        final int column = Math.min(this.columnOf(this.caret), this.lines().get(target).length());
        this.moveCaret(this.lineStart(target) + column, select);
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void setFocused(final boolean focused) {
        if (this.focused != focused) {
            WLInput.textFocusChanged(this, focused);
        }
        this.focused = focused;
        this.caretTimer = 0f;
    }

    public boolean isOver(final double mouseX, final double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.w && mouseY >= this.y && mouseY < this.y + this.h;
    }

    private float lineHeight() {
        return this.textSize * LINE_SPACING;
    }

    private float inset() {
        return TEXT_INSET * this.uiScale;
    }

    private float scrollbarWidth() {
        return SCROLLBAR_W * this.uiScale;
    }

    private int indexAt(final double mouseX, final double mouseY) {
        final List<String> all = this.lines();
        final float lineH = this.lineHeight();
        final int line = Math.max(0, Math.min(all.size() - 1, (int) Math.floor((mouseY - (this.y + this.inset()) + this.scrollY) / lineH)));
        final String content = all.get(line);
        final float textX = this.x + this.inset() - this.scrollX;
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i <= content.length(); i++) {
            final float cx = textX + Fonts.SFPRO_DISPLAY.width(content.substring(0, i), this.textSize);
            final float distance = Math.abs(cx - (float) mouseX);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return this.lineStart(line) + best;
    }

    public void mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        this.setFocused(true);
        if (this.isOverThumb(event.x(), event.y())) {
            this.draggingThumb = true;
            this.thumbGrabOffset = (float) event.y() - this.thumbTop();
            return;
        }
        if (doubleClick) {
            this.selectLine(this.lineOf(this.indexAt(event.x(), event.y())));
            return;
        }
        this.moveCaret(this.indexAt(event.x(), event.y()), false);
    }

    private void selectLine(final int line) {
        final int start = this.lineStart(line);
        this.selectionAnchor = start;
        this.caret = start + this.lines().get(line).length();
    }

    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.draggingThumb) {
            final float travel = (this.h - this.inset() * 2f) - this.thumbHeight();
            final float t = travel <= 0f ? 0f : ((float) event.y() - this.thumbGrabOffset - (this.y + this.inset())) / travel;
            this.scrollY = Math.max(0f, Math.min(this.maxScrollY(), t * this.maxScrollY()));
            this.scrollTarget = this.scrollY;
            return true;
        }
        if (!this.focused) {
            return false;
        }
        this.moveCaret(this.indexAt(event.x(), event.y()), true);
        return true;
    }

    public void mouseReleased() {
        this.draggingThumb = false;
    }

    public boolean mouseScrolled(final double scrollAmount) {
        this.scrollTarget = Math.max(0f, Math.min(this.maxScrollY(), this.scrollTarget - (float) scrollAmount * this.lineHeight() * 3f));
        return true;
    }

    public boolean keyPressed(final KeyEvent event) {
        if (!this.focused) {
            return false;
        }
        final int key = event.key();
        final boolean ctrl = event.hasControlDown();
        final boolean shift = event.hasShiftDown();
        if (ctrl) {
            switch (key) {
                case InputConstants.KEY_A -> {
                    this.selectionAnchor = 0;
                    this.caret = this.text.length();
                    return true;
                }
                case InputConstants.KEY_C -> {
                    this.copy();
                    return true;
                }
                case InputConstants.KEY_X -> {
                    this.copy();
                    this.deleteSelection();
                    return true;
                }
                case InputConstants.KEY_V -> {
                    final String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                    this.insert(clip);
                    return true;
                }
                default -> {
                }
            }
        }
        switch (key) {
            case InputConstants.KEY_BACKSPACE -> this.backspace();
            case InputConstants.KEY_DELETE -> this.deleteForward();
            case InputConstants.KEY_LEFT -> this.moveCaret(this.caret - 1, shift);
            case InputConstants.KEY_RIGHT -> this.moveCaret(this.caret + 1, shift);
            case InputConstants.KEY_UP -> this.moveLine(-1, shift);
            case InputConstants.KEY_DOWN -> this.moveLine(1, shift);
            case InputConstants.KEY_PAGEUP -> this.moveLine(-this.visibleLines(), shift);
            case InputConstants.KEY_PAGEDOWN -> this.moveLine(this.visibleLines(), shift);
            case InputConstants.KEY_HOME -> this.moveCaret(ctrl ? 0 : this.lineStart(this.lineOf(this.caret)), shift);
            case InputConstants.KEY_END -> {
                final int line = this.lineOf(this.caret);
                this.moveCaret(ctrl ? this.text.length() : this.lineStart(line) + this.lines().get(line).length(), shift);
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> this.insert("\n");
            case InputConstants.KEY_TAB -> this.insert(TAB);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void copy() {
        if (this.hasSelection()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.text.substring(this.selStart(), this.selEnd()));
        }
    }

    public boolean charTyped(final CharacterEvent event) {
        if (!this.focused) {
            return false;
        }
        final int cp = event.codepoint();
        if (cp < 32 || cp == 127) {
            return false;
        }
        this.insert(new String(Character.toChars(cp)));
        return true;
    }

    private int visibleLines() {
        return Math.max(1, (int) Math.floor((this.h - this.inset() * 2f) / this.lineHeight()));
    }

    private float maxScrollY() {
        return Math.max(0f, this.lines().size() * this.lineHeight() - (this.h - this.inset() * 2f));
    }

    private void followCaret() {
        final float max = this.maxScrollY();
        this.scrollY = Math.max(0f, Math.min(max, this.scrollY));
        this.scrollTarget = Math.max(0f, Math.min(max, this.scrollTarget));
        if (this.draggingThumb || (this.caret == this.followedCaret && this.text.length() == this.followedLength)) {
            return;
        }
        this.followedCaret = this.caret;
        this.followedLength = this.text.length();
        final int line = this.lineOf(this.caret);
        final float lineH = this.lineHeight();
        final float viewH = this.h - this.inset() * 2f;
        final float top = line * lineH;
        if (top < this.scrollTarget) {
            this.scrollTarget = top;
            this.scrollY = this.scrollTarget;
        } else if (top + lineH > this.scrollTarget + viewH) {
            this.scrollTarget = Math.max(0f, Math.min(max, top + lineH - viewH));
            this.scrollY = this.scrollTarget;
        }
        final String content = this.lines().get(line);
        final float caretX = Fonts.SFPRO_DISPLAY.width(content.substring(0, Math.min(this.columnOf(this.caret), content.length())), this.textSize);
        final float viewW = this.w - this.inset() * 2f - this.scrollbarWidth();
        if (caretX < this.scrollX) {
            this.scrollX = caretX;
        } else if (caretX > this.scrollX + viewW) {
            this.scrollX = caretX - viewW;
        }
        this.scrollX = Math.max(0f, this.scrollX);
    }

    public void extract(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final int mouseX, final int mouseY, final float dt, final float alpha, final float textSize, final float uiScale) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.textSize = textSize;
        this.uiScale = Math.max(0.01f, uiScale);
        this.caretTimer += Math.max(0f, dt);
        this.followCaret();
        if (!this.draggingThumb) {
            this.scrollY = this.glide.step(this.scrollY, this.scrollTarget, SCROLL_GLIDE_SPEED, dt);
        }
        final float shown = ScrollGlide.shown(this.scrollY);
        final float radius = Math.min(8f, Math.min(w, h) * 0.1f);
        final int well = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().shadowColor(), 0.30f), alpha);
        final int edge = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().label(), this.focused ? 0.35f : 0.16f), alpha);
        final float border = Math.max(this.uiScale, textSize * 0.12f);
        graphics.fillRoundedGradient(x, y, x + w, y + h, edge, edge, edge, edge, radius);
        graphics.fillRoundedGradient(x + border, y + border, x + w - border, y + h - border, well, well, well, well, Math.max(0f, radius - border));
        WLRenderSystem.cursors().requestIf(this.isOver(mouseX, mouseY), Cursors.Cursor.TEXT);
        final float lineH = this.lineHeight();
        final float textLeft = x + this.inset() - this.scrollX;
        final float viewTop = y + this.inset();
        final float viewBottom = y + h - this.inset();
        final List<String> all = this.lines();
        final int first = Math.max(0, (int) Math.floor(shown / lineH));
        final int last = Math.min(all.size() - 1, (int) Math.ceil((shown + (viewBottom - viewTop)) / lineH));
        graphics.enableScissor(Math.round(x + 1f), Math.round(viewTop), Math.round(x + w - this.scrollbarWidth()), Math.round(viewBottom));
        try {
            final int selFrom = this.selStart();
            final int selTo = this.selEnd();
            for (int line = first; line <= last; line++) {
                final String content = all.get(line);
                final float ly = viewTop + line * lineH - shown;
                this.extractSelection(graphics, content, line, selFrom, selTo, textLeft, ly, lineH, alpha);
                Fonts.SFPRO_DISPLAY.extract(graphics, content, textLeft, Fonts.SFPRO_DISPLAY.topForBaseline(ly + textSize * 0.8f, textSize), textSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
            }
            this.extractCaret(graphics, textLeft, viewTop, lineH, alpha);
        } finally {
            graphics.disableScissor();
        }
        this.extractScrollbar(graphics, alpha);
    }

    private void extractSelection(final WLGraphics2D graphics, final String content, final int line, final int selFrom, final int selTo, final float textLeft, final float ly, final float lineH, final float alpha) {
        if (selFrom == selTo) {
            return;
        }
        final int start = this.lineStart(line);
        final int end = start + content.length();
        if (selTo < start || selFrom > end) {
            return;
        }
        final int from = Math.max(0, selFrom - start);
        final int to = Math.min(content.length(), selTo - start);
        if (to < from) {
            return;
        }
        final float x0 = textLeft + Fonts.SFPRO_DISPLAY.width(content.substring(0, from), this.textSize);
        float x1 = textLeft + Fonts.SFPRO_DISPLAY.width(content.substring(0, to), this.textSize);
        if (selTo > end) {
            x1 += this.textSize * 0.4f;
        }
        final int fill = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().accent(), 0.35f), alpha);
        graphics.fill(x0, ly, Math.max(x0 + 1f, x1), ly + lineH, fill, null);
    }

    private void extractCaret(final WLGraphics2D graphics, final float textLeft, final float viewTop, final float lineH, final float alpha) {
        if (!this.focused || (this.caretTimer % BLINK_PERIOD) >= BLINK_PERIOD / 2f) {
            return;
        }
        final int line = this.lineOf(this.caret);
        final String content = this.lines().get(line);
        final int column = Math.min(this.columnOf(this.caret), content.length());
        final float cy = viewTop + line * lineH - ScrollGlide.shown(this.scrollY);
        final int color = ColorUtils.withAlpha(Theme.get().label(), alpha);
        final float width = DevicePixels.snapSizeX(Math.max(0.5f * this.uiScale, this.textSize * CARET_WIDTH_RATIO));
        final float cx = DevicePixels.snapX(textLeft + Fonts.SFPRO_DISPLAY.width(content.substring(0, column), this.textSize), graphics.pose());
        final float inset = lineH * 0.1f;
        graphics.fill(cx, cy + inset, cx + width, cy + lineH - inset, color, null);
    }

    private float thumbHeight() {
        final float viewH = this.h - this.inset() * 2f;
        return Math.max(12f * this.uiScale, viewH * (viewH / (viewH + this.maxScrollY())));
    }

    private float thumbTop() {
        final float max = this.maxScrollY();
        final float viewH = this.h - this.inset() * 2f;
        final float t = max <= 0f ? 0f : this.scrollY / max;
        return this.y + this.inset() + (viewH - this.thumbHeight()) * t;
    }

    private float thumbLeft() {
        return this.x + this.w - this.scrollbarWidth() - SCROLLBAR_INSET * this.uiScale;
    }

    private boolean isOverThumb(final double mouseX, final double mouseY) {
        if (this.maxScrollY() <= 0f) {
            return false;
        }
        final float left = this.thumbLeft() - 2f * this.uiScale;
        final float top = this.thumbTop();
        return mouseX >= left && mouseX < this.x + this.w && mouseY >= top && mouseY < top + this.thumbHeight();
    }

    private void extractScrollbar(final WLGraphics2D graphics, final float alpha) {
        if (this.maxScrollY() <= 0f) {
            return;
        }
        final float width = this.scrollbarWidth();
        final float thumbH = this.thumbHeight();
        final float thumbY = this.thumbTop();
        final float barX = this.thumbLeft();
        final int color = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().label(), this.draggingThumb ? 0.5f : 0.3f), alpha);
        graphics.fillRoundedGradient(barX, thumbY, barX + width, thumbY + thumbH, color, color, color, color, width / 2f);
    }
}
