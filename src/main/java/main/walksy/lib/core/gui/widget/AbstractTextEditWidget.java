package main.walksy.lib.core.gui.widget;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.platform.WLInput;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class AbstractTextEditWidget extends WLWidget {
    protected static final float BLINK_PERIOD = 1.0f;
    protected static final float FIELD_HOVER_SPEED = 14f;
    protected static final float FIELD_FOCUS_SPEED = 12f;
    protected static final float FIELD_CORNER_RATIO = 0.3f;
    protected static final float CARET_WIDTH = 0.5f;
    protected static final float CARET_INSET = 2f;
    protected final StringBuilder text = new StringBuilder();
    protected int caret;
    protected int selectionAnchor;
    protected int displayPos;
    protected int lastVisibleEnd;
    protected int maxLength = 256;
    protected float caretTimer;
    private float fieldHoverAnim;
    private float fieldFocusAnim;
    private float lastTextStartX;
    private int lastDisplayPos;
    private float lastSize = 1f;

    protected static int SELECTION_COLOR() {
        return Theme.get().accent();
    }

    protected AbstractTextEditWidget(final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
    }

    protected void extractFieldGlass(final WLGraphics2D graphics, final GlassStyle style, final float alpha, final float uiScale, final float dt) {
        this.extractFieldGlass(graphics, style, alpha, uiScale, dt, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    protected void extractFieldGlass(final WLGraphics2D graphics, final GlassStyle style, final float alpha, final float uiScale, final float dt, final float x, final float y, final float width, final float height) {
        this.caretTimer += dt;
        final boolean hovered = this.isHovered() && this.active;
        final boolean focused = this.isFocused() && this.active;
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.TEXT);
        WLRenderSystem.cursors().requestIf(this.isHovered() && !this.active, Cursors.Cursor.NOT_ALLOWED);
        this.fieldHoverAnim = MathUtils.approach(this.fieldHoverAnim, hovered ? 1f : 0f, FIELD_HOVER_SPEED, dt);
        this.fieldFocusAnim = MathUtils.approach(this.fieldFocusAnim, focused ? 1f : 0f, FIELD_FOCUS_SPEED, dt);
        graphics.glass()
            .dimensions(x, y, width, height)
            .cornerRadius(height * FIELD_CORNER_RATIO)
            .style(this.fieldMaterial(style, height, uiScale).hoverDarken(this.fieldHoverAnim).scaled(uiScale).opacity(alpha))
            .focus(this.fieldFocusAnim)
            .extract();
    }

    private GlassStyle fieldMaterial(final GlassStyle style, final float height, final float uiScale) {
        final GlassStyle resting = flatten(style.copy());
        if (this.fieldFocusAnim <= 0.001f) {
            return resting;
        }
        final GlassStyle focused = SearchBar.chromeStyle().blurRadius(style.getBlurRadius());
        SearchBar.sizeChromeLens(focused, height, uiScale);
        if (this.fieldFocusAnim >= 0.999f) {
            return carryOver(focused, style);
        }
        return carryOver(GlassStyle.lerp(resting, focused, this.fieldFocusAnim), style);
    }

    private static GlassStyle carryOver(final GlassStyle blended, final GlassStyle source) {
        blended.layer(source.getLayer());
        if (source.hasFadeKey()) {
            blended.fadeKey(source.getFadeKey());
        }
        return blended;
    }

    private static GlassStyle flatten(final GlassStyle style) {
        return style.flat();
    }

    protected void extractCaretAndSelection(final WLGraphics2D graphics, final String prefix, final float textStartX, final float size, final float alpha, final float uiScale) {
        this.extractCaretAndSelection(graphics, prefix, textStartX, size, alpha, uiScale, this.getY(), this.getHeight());
    }

    protected void extractCaretAndSelection(final WLGraphics2D graphics, final String prefix, final float textStartX, final float size, final float alpha, final float uiScale, final float fieldY, final float fieldHeight) {
        final boolean focused = this.isFocused() && this.active;
        final String value = this.text.toString();
        final float snappedTextStartX = snapToPhysicalPixel(textStartX);
        final float top = snapToPhysicalPixelY(fieldY + CARET_INSET * uiScale);
        final float bottom = snapToPhysicalPixelY(fieldY + fieldHeight - CARET_INSET * uiScale);
        if (this.hasSelection()) {
            final int s = Math.max(this.selStart(), this.displayPos);
            final int e = Math.min(this.selEnd(), this.lastVisibleEnd);
            if (e > s) {
                final float x1 = snapToPhysicalPixel(snappedTextStartX + Fonts.SFPRO_DISPLAY.width(prefix + value.substring(this.displayPos, s), size));
                final float x2 = snapToPhysicalPixel(snappedTextStartX + Fonts.SFPRO_DISPLAY.width(prefix + value.substring(this.displayPos, e), size));
                graphics.fill(x1, top, x2, bottom, ColorUtils.withAlpha(SELECTION_COLOR(), 0.35f * alpha), null);
            }
        }
        if (focused && this.caret >= this.displayPos && this.caret <= this.lastVisibleEnd && this.caretVisible()) {
            final String upToCaret = prefix + value.substring(this.displayPos, this.caret);
            final float cx = snapToPhysicalPixel(snappedTextStartX + Fonts.SFPRO_DISPLAY.width(upToCaret, size));
            graphics.fill(cx, top, cx + caretWidth(uiScale), bottom, ColorUtils.withAlpha(Theme.get().label(), alpha), null);
        }
    }

    protected static float caretWidth(final float uiScale) {
        return DevicePixels.snapSizeX(CARET_WIDTH * uiScale);
    }

    protected void onTextChanged() {
    }

    protected String layoutText(final float textStartX, final float innerWidth, final float size) {
        final String value = this.text.toString();
        this.caret = Math.max(0, Math.min(this.caret, value.length()));
        this.selectionAnchor = Math.max(0, Math.min(this.selectionAnchor, value.length()));
        if (this.caret < this.displayPos) {
            this.displayPos = this.caret;
        }
        while (this.displayPos < this.caret && Fonts.SFPRO_DISPLAY.width(value.substring(this.displayPos, this.caret), size) > innerWidth) {
            this.displayPos++;
        }
        this.displayPos = Math.max(0, Math.min(this.displayPos, value.length()));
        int visEnd = this.displayPos;
        while (visEnd < value.length() && Fonts.SFPRO_DISPLAY.width(value.substring(this.displayPos, visEnd + 1), size) <= innerWidth) {
            visEnd++;
        }
        this.lastTextStartX = textStartX;
        this.lastDisplayPos = this.displayPos;
        this.lastSize = size;
        this.lastVisibleEnd = visEnd;
        return value.substring(this.displayPos, visEnd);
    }

    protected void insertText(final String raw) {
        String s = sanitize(raw);
        if (s.isEmpty()) {
            return;
        }
        if (this.hasSelection()) {
            this.deleteSelection();
        }
        final int space = this.maxLength - this.text.length();
        if (space <= 0) {
            return;
        }
        if (s.length() > space) {
            s = s.substring(0, space);
        }
        this.text.insert(this.caret, s);
        this.caret += s.length();
        this.selectionAnchor = this.caret;
        this.caretTimer = 0f;
        this.onTextChanged();
    }

    protected void backspace(final boolean word) {
        if (this.hasSelection()) {
            this.deleteSelection();
            this.onTextChanged();
            return;
        }
        if (this.caret == 0) {
            return;
        }
        final int from = word ? this.prevWordBoundary(this.caret) : this.caret - 1;
        this.text.delete(from, this.caret);
        this.caret = from;
        this.selectionAnchor = this.caret;
        this.caretTimer = 0f;
        this.onTextChanged();
    }

    protected void deleteForward(final boolean word) {
        if (this.hasSelection()) {
            this.deleteSelection();
            this.onTextChanged();
            return;
        }
        if (this.caret >= this.text.length()) {
            return;
        }
        final int to = word ? this.nextWordBoundary(this.caret) : this.caret + 1;
        this.text.delete(this.caret, to);
        this.selectionAnchor = this.caret;
        this.caretTimer = 0f;
        this.onTextChanged();
    }

    protected void deleteSelection() {
        final int s = this.selStart();
        final int e = this.selEnd();
        this.text.delete(s, e);
        this.caret = s;
        this.selectionAnchor = s;
        this.caretTimer = 0f;
    }

    protected void moveCaret(final int pos, final boolean shift) {
        this.caret = Math.max(0, Math.min(pos, this.text.length()));
        if (!shift) {
            this.selectionAnchor = this.caret;
        }
        this.caretTimer = 0f;
    }

    protected void selectAll() {
        this.selectionAnchor = 0;
        this.caret = this.text.length();
        this.caretTimer = 0f;
    }

    protected void copySelection() {
        if (!this.hasSelection()) {
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(this.text.substring(this.selStart(), this.selEnd()));
    }

    protected void cutSelection() {
        if (!this.hasSelection()) {
            return;
        }
        this.copySelection();
        this.deleteSelection();
        this.onTextChanged();
    }

    protected void paste() {
        final String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip != null && !clip.isEmpty()) {
            this.insertText(clip);
        }
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        this.handleTextClick(event.x(), doubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (!this.isFocused() || !this.active || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        this.updateCaretFromDrag(event.x());
        return true;
    }

    protected void handleTextClick(final double mouseX, final boolean doubleClick) {
        if (doubleClick) {
            this.selectAll();
        } else {
            final int idx = this.indexAtX(mouseX);
            this.caret = idx;
            this.selectionAnchor = idx;
        }
        this.caretTimer = 0f;
    }

    protected void updateCaretFromDrag(final double mouseX) {
        this.caret = this.indexAtX(mouseX);
        this.caretTimer = 0f;
    }

    protected boolean handleCommonKeyPress(final KeyEvent event) {
        if (!this.isFocused() || !this.active) {
            return false;
        }
        final int key = event.key();
        final boolean ctrl = event.hasControlDown();
        final boolean shift = event.hasShiftDown();
        if (ctrl) {
            switch (key) {
                case InputConstants.KEY_A -> {
                    this.selectAll();
                    return true;
                }
                case InputConstants.KEY_C -> {
                    this.copySelection();
                    return true;
                }
                case InputConstants.KEY_X -> {
                    this.cutSelection();
                    return true;
                }
                case InputConstants.KEY_V -> {
                    this.paste();
                    return true;
                }
                default -> {
                }
            }
        }
        switch (key) {
            case InputConstants.KEY_BACKSPACE -> {
                this.backspace(ctrl);
                return true;
            }
            case InputConstants.KEY_DELETE -> {
                this.deleteForward(ctrl);
                return true;
            }
            case InputConstants.KEY_LEFT -> {
                this.moveCaret(ctrl ? this.prevWordBoundary(this.caret) : this.caret - 1, shift);
                return true;
            }
            case InputConstants.KEY_RIGHT -> {
                this.moveCaret(ctrl ? this.nextWordBoundary(this.caret) : this.caret + 1, shift);
                return true;
            }
            case InputConstants.KEY_HOME -> {
                this.moveCaret(0, shift);
                return true;
            }
            case InputConstants.KEY_END -> {
                this.moveCaret(this.text.length(), shift);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    protected boolean handleCharTyped(final CharacterEvent event) {
        if (!this.isFocused() || !this.active) {
            return false;
        }
        final int cp = event.codepoint();
        if (cp < 32 || cp == 127) {
            return false;
        }
        this.insertText(new String(Character.toChars(cp)));
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.handleCommonKeyPress(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.handleCharTyped(event);
    }

    @Override
    public void setFocused(final boolean focused) {
        final boolean was = this.isFocused();
        super.setFocused(focused);
        if (focused != was) {
            WLInput.textFocusChanged(this, focused);
        }
        if (focused && !was) {
            this.caretTimer = 0f;
        }
        if (!focused) {
            this.selectionAnchor = this.caret;
        }
    }

    public String getValue() {
        return this.text.toString();
    }

    public void setValue(final String value) {
        this.text.setLength(0);
        if (value != null) {
            this.text.append(sanitize(value));
        }
        this.caret = this.text.length();
        this.selectionAnchor = this.caret;
        this.displayPos = 0;
    }

    public boolean isEmpty() {
        return this.text.length() == 0;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = Math.max(1, maxLength);
    }

    protected boolean hasSelection() {
        return this.caret != this.selectionAnchor;
    }

    protected int selStart() {
        return Math.min(this.caret, this.selectionAnchor);
    }

    protected int selEnd() {
        return Math.max(this.caret, this.selectionAnchor);
    }

    protected boolean caretVisible() {
        return (this.caretTimer % BLINK_PERIOD) < BLINK_PERIOD / 2f;
    }

    protected int indexAtX(final double clickX) {
        final String value = this.text.toString();
        final int pos = Math.max(0, Math.min(this.lastDisplayPos, value.length()));
        final float local = (float) (clickX - this.lastTextStartX);
        if (local <= 0f) {
            return pos;
        }
        for (int i = pos; i < value.length(); i++) {
            final float wPrev = Fonts.SFPRO_DISPLAY.width(value.substring(pos, i), this.lastSize);
            final float wFull = Fonts.SFPRO_DISPLAY.width(value.substring(pos, i + 1), this.lastSize);
            if (local < (wPrev + wFull) / 2f) {
                return i;
            }
        }
        return value.length();
    }

    protected int prevWordBoundary(final int from) {
        int i = Math.max(0, Math.min(from, this.text.length()));
        while (i > 0 && !Character.isLetterOrDigit(this.text.charAt(i - 1))) {
            i--;
        }
        while (i > 0 && Character.isLetterOrDigit(this.text.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    protected int nextWordBoundary(final int from) {
        final int len = this.text.length();
        int i = Math.max(0, Math.min(from, len));
        while (i < len && !Character.isLetterOrDigit(this.text.charAt(i))) {
            i++;
        }
        while (i < len && Character.isLetterOrDigit(this.text.charAt(i))) {
            i++;
        }
        return i;
    }

    protected static String sanitize(final String s) {
        final StringBuilder out = new StringBuilder(s.length());
        s.codePoints().forEach(cp -> {
            if (cp >= 32 && cp != 127) {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }

    protected static float snapToPhysicalPixel(final float value) {
        return DevicePixels.snapX(value);
    }

    protected static float snapToPhysicalPixelY(final float value) {
        return DevicePixels.snapY(value);
    }
}
