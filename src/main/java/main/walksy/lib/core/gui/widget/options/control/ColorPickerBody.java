package main.walksy.lib.core.gui.widget.options.control;

import main.walksy.lib.core.WalksyLib;
import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.api.config.value.WLColor;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.AbstractTextEditWidget;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.util.Mth;

public final class ColorPickerBody {
    private static final float HOVER_SPEED = 14f;
    private static final float BOX_RADIUS = 6f;
    public static final int SWATCH_COLUMNS = 12;
    public static final int FADE_KEY_COUNT = 6 + SWATCH_COLUMNS * 2;
    private static final float FIELD_FILL_BOOST = 2.4f;
    private static final float RGB_GROUP_GAP = 6f;
    private static final float RGB_LABEL_W = 10f;
    private static final String[] RGB_LABELS = {"R", "G", "B"};
    private final Supplier<WLColor> color;
    private final HueConsumer onHue;
    private final BoxConsumer onBox;
    private final IntConsumer onHex;
    private final IntConsumer onSwatch;
    private final float uiScale;
    private final Metrics metrics;
    private final Layers layers;
    private final long swatchBlockBase;
    private final LiquidKnob2D boxKnob;
    private final LiquidKnob2D hueKnob;
    private final GlassStyle hexFieldStyle;
    private final HexField hexField;
    private final ChannelField[] channelFields = new ChannelField[3];
    private final float[] popularHoverAnim = new float[SWATCH_COLUMNS];
    private final float[] favoritesHoverAnim = new float[SWATCH_COLUMNS];
    private static final String FAVORITES_LABEL = "Favorites";

    public static int[] POPULAR_COLORS() {
        return Theme.get().systemPalette();
    }

    static {
        if (POPULAR_COLORS().length != SWATCH_COLUMNS) {
            throw new IllegalStateException("POPULAR_COLORS() must have exactly SWATCH_COLUMNS entries");
        }
    }

    public record Metrics(float boxKnobRestDiameter, float boxKnobPressDiameter, float hueKnobRestDiameter, float hueKnobPressDiameter, float hueHeight, float swatchGap, float swatchHoverGrow, float swatchRowLabelHeight, float swatchLabelGap, float swatchLabelSize, int swatchRowLabelColor) {

        public static Metrics rowPicker() {
            return new Metrics(9f, 13f, 9f, 18f, 10f, 3f, 0.6f, 10f, 3f, 9f, Theme.get().label());
        }

        public static Metrics embeddedPicker() {
            return new Metrics(9f, 13f, 9f, 16f, 10f, 3f, 0.6f, 10f, 3f, 9f, Theme.get().label());
        }
    }

    public record Layers(int knobLayer, int hexFieldLayer, int swatchLayer, int contentBoundaryLayer) {
    }

    public interface HueConsumer {
        void accept(float hue01);
    }

    public interface BoxConsumer {
        void accept(float saturation01, float brightness01);
    }

    public ColorPickerBody(final Supplier<WLColor> color, final HueConsumer onHue, final BoxConsumer onBox, final IntConsumer onHex, final IntConsumer onSwatch, final float uiScale, final long fadeKeyBase, final Metrics metrics, final Layers layers) {
        this.color = color;
        this.onHue = onHue;
        this.onBox = onBox;
        this.onHex = onHex;
        this.onSwatch = onSwatch;
        this.uiScale = uiScale;
        this.metrics = metrics;
        this.layers = layers;
        this.swatchBlockBase = fadeKeyBase + 6;
        final WLColor initial = color.get();
        this.boxKnob = new LiquidKnob2D(initial.getSaturation(), 1f - initial.getBrightness(), uiScale, fadeKeyBase, layers.knobLayer(), metrics.boxKnobRestDiameter(), metrics.boxKnobPressDiameter()).withoutInnerShadow();
        this.hueKnob = new LiquidKnob2D(initial.getHue(), 0.5f, uiScale, fadeKeyBase + 1, layers.knobLayer(), metrics.hueKnobRestDiameter(), metrics.hueKnobPressDiameter()).withoutInnerShadow();
        this.hexFieldStyle = fieldStyle(layers.hexFieldLayer(), fadeKeyBase + 2);
        this.hexField = new HexField();
        for (int i = 0; i < 3; i++) {
            this.channelFields[i] = new ChannelField(i, fieldStyle(layers.hexFieldLayer(), fadeKeyBase + 3 + i));
        }
    }

    private GlassStyle fieldStyle(final int layer, final long fadeKey) {
        return PopoverHost.surfaceStyle()
            .tint(Theme.get().cardFill(), Theme.tintAlpha(Theme.get().cardFillAlpha() * FIELD_FILL_BOOST))
            .layer(layer)
            .fadeKey(fadeKey);
    }

    public void extractBox(final WLGraphics2D graphics, final float dt, final float x0, final float x1, final float y0, final float y1, final boolean dragging, final int mouseX, final int mouseY, final float alpha) {
        final WLColor c = this.color.get();
        final int white = ColorUtils.withAlpha(0xFFFFFFFF, alpha);
        final int hue = ColorUtils.withAlpha(WLColor.getHSBColor(c.getHue(), 1f, 1f).getRGB() | 0xFF000000, alpha);
        final int black = ColorUtils.withAlpha(0xFF000000, alpha);
        WLRenderSystem.contentStratum().beginPopover(this.layers.contentBoundaryLayer());
        try {
            graphics.fillRoundedGradient(x0, y0, x1, y1, white, hue, black, black, BOX_RADIUS * this.uiScale);
        } finally {
            WLRenderSystem.contentStratum().endPopover();
        }
        final float inset = this.metrics.boxKnobRestDiameter() * this.uiScale / 2f;
        this.boxKnob.extract(graphics, dt, c.getSaturation(), 1f - c.getBrightness(), dragging, c.isPulse(), x0 + inset, x1 - inset, y0 + inset, y1 - inset, alpha);
    }

    public void applyBox(final double mouseX, final double mouseY, final float x0, final float x1, final float y0, final float y1) {
        final float sat = Mth.clamp((float) ((mouseX - x0) / (x1 - x0)), 0f, 1f);
        final float v = Mth.clamp(1f - (float) ((mouseY - y0) / (y1 - y0)), 0f, 1f);
        this.onBox.accept(sat, v);
    }

    public void extractHueStrip(final WLGraphics2D graphics, final float dt, final float x0, final float x1, float y0, final boolean dragging, final int mouseX, final int mouseY, final float alpha) {
        final WLColor c = this.color.get();
        y0 = WLScreen.snapPhysical(y0);
        final float y1 = WLScreen.snapPhysical(y0 + this.metrics.hueHeight() * this.uiScale);
        final float r = (y1 - y0) / 2f;
        final int[] stops = new int[7];
        for (int i = 0; i <= 6; i++) {
            stops[i] = ColorUtils.withAlpha(WLColor.getHSBColor(i / 6f, 1f, 1f).getRGB() | 0xFF000000, alpha);
        }
        WLRenderSystem.contentStratum().beginPopover(this.layers.contentBoundaryLayer());
        try {
            graphics.fillRoundedGradient(x0, y0, x0 + r * 2f, y1, stops[0], stops[0], stops[0], stops[0], r);
            graphics.fillRoundedGradient(x1 - r * 2f, y0, x1, y1, stops[6], stops[6], stops[6], stops[6], r);
            final float gx0 = x0 + r;
            final float segW = (x1 - r - gx0) / 6f;
            for (int i = 0; i < 6; i++) {
                graphics.fillRoundedGradient(gx0 + i * segW, y0, gx0 + (i + 1) * segW, y1, stops[i], stops[i + 1], stops[i + 1], stops[i], 0f);
            }
        } finally {
            WLRenderSystem.contentStratum().endPopover();
        }
        final float insetX = this.metrics.hueKnobRestDiameter() * this.uiScale / 2f;
        final float knobY = (y0 + y1) / 2f;
        this.hueKnob.extract(graphics, dt, c.getHue(), 0.5f, dragging, c.isRainbow(), x0 + insetX, x1 - insetX, knobY, knobY, alpha);
    }

    public void applyHue(final double mouseX, final float x0, final float x1) {
        this.onHue.accept(Mth.clamp((float) ((mouseX - x0) / (x1 - x0)), 0f, 1f));
    }

    public void extractHexField(final WLGraphics2D graphics, final float x0, final float y0, final float w, final float h, final int mouseX, final int mouseY, final float delta, final float alpha) {
        this.hexField.setX(Math.round(x0));
        this.hexField.setY(Math.round(y0));
        this.hexField.setWidth(Math.round(w));
        this.hexField.setHeight(Math.round(h));
        if (!this.hexField.isFocused()) {
            final WLColor c = this.color.get();
            this.hexField.setValue(String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()));
        }
        this.hexField.setAlpha(alpha);
        graphics.widget(this.hexField, mouseX, mouseY, delta);
    }

    public void extractRgbRow(final WLGraphics2D graphics, final float x0, final float x1, final float y, final float rowH, final int mouseX, final int mouseY, final float delta, final float alpha) {
        final WLColor c = this.color.get();
        final float groupW = (x1 - x0 - RGB_GROUP_GAP * this.uiScale * 2f) / 3f;
        final float labelW = RGB_LABEL_W * this.uiScale;
        final float fieldW = groupW - labelW;
        final float textSize = OptionRowWidget.LABEL_SIZE * this.uiScale;
        final float capHeight = textSize * 0.59043f;
        final float baseline = y + rowH / 2f + capHeight * 0.5f;
        final float labelTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, textSize);
        final int[] values = {c.getRed(), c.getGreen(), c.getBlue()};
        for (int i = 0; i < 3; i++) {
            final float gx0 = x0 + i * (groupW + RGB_GROUP_GAP * this.uiScale);
            Fonts.SFPRO_DISPLAY.extract(graphics, RGB_LABELS[i], gx0, labelTop, textSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
            final ChannelField field = this.channelFields[i];
            field.setX(Math.round(gx0 + labelW));
            field.setY(Math.round(y));
            field.setWidth(Math.round(fieldW));
            field.setHeight(Math.round(rowH));
            if (!field.isFocused()) {
                field.setValue(String.valueOf(values[i]));
            }
            field.setAlpha(alpha);
            graphics.widget(field, mouseX, mouseY, delta);
        }
    }

    public boolean fieldClicked(final MouseButtonEvent event, final boolean doubleClick, final double mouseX, final double mouseY) {
        AbstractTextEditWidget hit = null;
        for (final AbstractTextEditWidget field : this.fields()) {
            if (mouseX >= field.getX() && mouseX < field.getX() + field.getWidth() && mouseY >= field.getY() && mouseY < field.getY() + field.getHeight()) {
                hit = field;
            }
        }
        for (final AbstractTextEditWidget field : this.fields()) {
            field.setFocused(field == hit);
        }
        if (hit == null) {
            return false;
        }
        hit.onClick(event, doubleClick);
        return true;
    }

    public void clearFieldFocus() {
        for (final AbstractTextEditWidget field : this.fields()) {
            field.setFocused(false);
        }
    }

    public boolean isFieldFocused() {
        return this.focusedField() != null;
    }

    public boolean fieldDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        final AbstractTextEditWidget focused = this.focusedField();
        return focused != null && focused.mouseDragged(event, dragX, dragY);
    }

    public boolean fieldKeyPressed(final KeyEvent event) {
        final AbstractTextEditWidget focused = this.focusedField();
        return focused != null && focused.keyPressed(event);
    }

    public boolean fieldCharTyped(final CharacterEvent event) {
        final AbstractTextEditWidget focused = this.focusedField();
        return focused != null && focused.charTyped(event);
    }

    private AbstractTextEditWidget[] fields() {
        return new AbstractTextEditWidget[]{this.hexField, this.channelFields[0], this.channelFields[1], this.channelFields[2]};
    }

    private AbstractTextEditWidget focusedField() {
        for (final AbstractTextEditWidget field : this.fields()) {
            if (field.isFocused()) {
                return field;
            }
        }
        return null;
    }

    private final class ChannelField extends AbstractTextEditWidget {
        private final int channel;
        private final GlassStyle style;

        ChannelField(final int channel, final GlassStyle style) {
            super(0, 0, 1, 1);
            this.channel = channel;
            this.style = style;
            this.setMaxLength(3);
        }

        @Override
        protected void insertText(final String raw) {
            final StringBuilder digits = new StringBuilder(raw.length());
            for (int i = 0; i < raw.length(); i++) {
                final char ch = raw.charAt(i);
                if (Character.isDigit(ch)) {
                    digits.append(ch);
                }
            }
            super.insertText(digits.toString());
        }

        @Override
        protected void onTextChanged() {
            final String text = this.getValue();
            if (text.isEmpty()) {
                return;
            }
            final int parsed;
            try {
                parsed = Integer.parseInt(text);
            } catch (final NumberFormatException ignored) {
                return;
            }
            final int clamped = Math.max(0, Math.min(255, parsed));
            final WLColor current = ColorPickerBody.this.color.get();
            final int r = this.channel == 0 ? clamped : current.getRed();
            final int g = this.channel == 1 ? clamped : current.getGreen();
            final int b = this.channel == 2 ? clamped : current.getBlue();
            ColorPickerBody.this.onHex.accept((r << 16) | (g << 8) | b);
        }

        @Override
        protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
            final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
            if (alpha <= 0.001f) {
                return;
            }
            final float dt = WLRenderSystem.frameDeltaSeconds();
            this.extractFieldGlass(graphics, this.style, alpha, ColorPickerBody.this.uiScale, dt);
            final float textStartX = this.getX() + 4f * ColorPickerBody.this.uiScale;
            final float innerWidth = Math.max(4f, this.getWidth() - 8f * ColorPickerBody.this.uiScale);
            final float size = Math.min(OptionRowWidget.LABEL_SIZE * ColorPickerBody.this.uiScale, this.getHeight() * 0.55f);
            final String visible = this.layoutText(textStartX, innerWidth, size);
            final float capHeight = size * 0.7f;
            final float baseline = this.getY() + this.getHeight() / 2f + capHeight * 0.5f;
            final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size);
            if (!visible.isEmpty()) {
                Fonts.SFPRO_DISPLAY.extract(graphics, visible, textStartX, textTop, size, ColorUtils.withAlpha(Theme.get().label(), alpha));
            }
            this.extractCaretAndSelection(graphics, "", textStartX, size, alpha, ColorPickerBody.this.uiScale);
        }
    }

    private final class HexField extends AbstractTextEditWidget {
        HexField() {
            super(0, 0, 1, 1);
            this.setMaxLength(6);
        }

        @Override
        protected void onTextChanged() {
            final String hex = this.getValue().replace("#", "").trim();
            if (hex.length() != 6) {
                return;
            }
            try {
                ColorPickerBody.this.onHex.accept(Integer.parseInt(hex, 16));
            } catch (final NumberFormatException ignored) {
            }
        }

        @Override
        protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
            final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
            if (alpha <= 0.001f) {
                return;
            }
            final float dt = WLRenderSystem.frameDeltaSeconds();
            this.extractFieldGlass(graphics, ColorPickerBody.this.hexFieldStyle, alpha, ColorPickerBody.this.uiScale, dt);
            final float textStartX = this.getX() + 5f * ColorPickerBody.this.uiScale;
            final float innerWidth = Math.max(4f, this.getWidth() - 10f * ColorPickerBody.this.uiScale);
            final float size = Math.min(OptionRowWidget.LABEL_SIZE * ColorPickerBody.this.uiScale, this.getHeight() * 0.55f);
            final String visible = this.layoutText(textStartX, innerWidth, size);
            final float capHeight = size * 0.7f;
            final float baseline = this.getY() + this.getHeight() / 2f + capHeight * 0.5f;
            final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size);
            if (!visible.isEmpty()) {
                Fonts.SFPRO_DISPLAY.extract(graphics, "#" + visible, textStartX, textTop, size, ColorUtils.withAlpha(Theme.get().label(), alpha));
            }
            this.extractCaretAndSelection(graphics, "#", textStartX, size, alpha, ColorPickerBody.this.uiScale);
        }
    }

    private static GlassStyle swatchCellStyle() {
        return new GlassStyle()
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .refractionStrength(0f)
            .refractionThickness(0f)
            .refractionDispersion(0f)
            .refractionReach(0f)
            .fresnelFactor(0f)
            .fresnelRange(0f)
            .glareFactor(0f)
            .glareRange(0f)
            .edgeDarken(0f)
            .brightEdge(0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .innerShadow(0f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
    }

    private void extractSwatchCell(final WLGraphics2D graphics, final float x, final float y, final float size, final int argb, final float hoverAnim, final long fadeKey, final float alpha) {
        final float grow = hoverAnim * this.metrics.swatchHoverGrow() * this.uiScale;
        final float gx = x - grow;
        final float gy = y - grow;
        final float gsize = size + grow * 2f;
        graphics.glass()
            .dimensions(gx, gy, gsize, gsize)
            .cornerRadius(gsize * 0.28f)
            .style(swatchCellStyle()
                .scaled(this.uiScale)
                .fadeKey(fadeKey)
                .layer(this.layers.swatchLayer())
                .opacity(alpha)
                .tint(argb & 0x00FFFFFF, 1.25f))
            .extract();
    }

    private boolean isOverCell(final double mouseX, final double mouseY, final float x, final float y, final float size) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    private static int swatchIndexAt(final double mouseX, final float x0, final float swSize, final float gap) {
        final float rel = (float) (mouseX - x0);
        if (rel < 0) {
            return -1;
        }
        final float period = swSize + gap;
        final int idx = (int) (rel / period);
        final float withinCell = rel - idx * period;
        return withinCell <= swSize ? idx : -1;
    }

    public void extractSwatchLabel(final WLGraphics2D graphics, final String text, final float x0, final float y, final float alpha) {
        final float size = this.metrics.swatchLabelSize() * this.uiScale;
        final float capHeight = size * 0.59043f;
        final float baseline = y + this.metrics.swatchRowLabelHeight() * this.uiScale / 2f + capHeight * 0.5f;
        Fonts.SFPRO_DISPLAY.extract(graphics, text, x0, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(this.metrics.swatchRowLabelColor(), alpha));
    }

    public void extractFavoritesLabel(final WLGraphics2D graphics, final float x0, final float y, final int mouseX, final int mouseY, final float alpha) {
        this.extractSwatchLabel(graphics, FAVORITES_LABEL, x0, y, alpha);
    }

    public void extractPopularRow(final WLGraphics2D graphics, final float dt, final float x0, final float y, final float swSize, final int mouseX, final int mouseY, final float alpha) {
        final float gap = this.metrics.swatchGap() * this.uiScale;
        for (int i = 0; i < POPULAR_COLORS().length; i++) {
            final float cx0 = x0 + i * (swSize + gap);
            final boolean hovered = this.isOverCell(mouseX, mouseY, cx0, y, swSize);
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            this.popularHoverAnim[i] = MathUtils.approach(this.popularHoverAnim[i], hovered ? 1f : 0f, HOVER_SPEED, dt);
            this.extractSwatchCell(graphics, cx0, y, swSize, POPULAR_COLORS()[i], this.popularHoverAnim[i], this.swatchBlockBase + i, alpha);
        }
    }

    public boolean handlePopularRowClick(final MouseButtonEvent event, final double mouseX, final double mouseY, final float x0, final float y, final float swSize) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || mouseY < y || mouseY >= y + swSize) {
            return false;
        }
        final int idx = swatchIndexAt(mouseX, x0, swSize, this.metrics.swatchGap() * this.uiScale);
        if (idx >= 0 && idx < POPULAR_COLORS().length) {
            this.onSwatch.accept(POPULAR_COLORS()[idx]);
        }
        return true;
    }

    public void extractFavoritesRow(final WLGraphics2D graphics, final float dt, final float x0, final float y, final float swSize, final int mouseX, final int mouseY, final float alpha) {
        final List<Integer> favorites = WalksyLib.getInstance().getFavoriteColors().get();
        final float gap = this.metrics.swatchGap() * this.uiScale;
        final boolean tileHovered = this.isOverCell(mouseX, mouseY, x0, y, swSize);
        WLRenderSystem.cursors().requestIf(tileHovered, Cursors.Cursor.POINTER);
        this.favoritesHoverAnim[0] = MathUtils.approach(this.favoritesHoverAnim[0], tileHovered ? 1f : 0f, HOVER_SPEED, dt);
        final float grow = this.favoritesHoverAnim[0] * this.metrics.swatchHoverGrow() * this.uiScale;
        final float gx = x0 - grow;
        final float gy = y - grow;
        final float gsize = swSize + grow * 2f;
        final long tileFadeKey = this.swatchBlockBase + SWATCH_COLUMNS * 2L - 1L;
        graphics.glass()
            .dimensions(gx, gy, gsize, gsize)
            .cornerRadius(gsize * 0.28f)
            .style(swatchCellStyle()
                .scaled(this.uiScale)
                .fadeKey(tileFadeKey)
                .layer(this.layers.swatchLayer())
                .opacity(alpha)
                .tint(Theme.get().controlTint(), Theme.get().fillPrimary()))
            .extract();
        final float iconSize = gsize * 0.4f;
        Icons.PLUS.extract(graphics, gx + (gsize - iconSize) / 2f, gy + (gsize - iconSize) / 2f, iconSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
        final long favBase = this.swatchBlockBase + SWATCH_COLUMNS;
        for (int i = 0; i < favorites.size(); i++) {
            final float cx0 = x0 + (i + 1) * (swSize + gap);
            final boolean hovered = this.isOverCell(mouseX, mouseY, cx0, y, swSize);
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            final int slot = i + 1;
            this.favoritesHoverAnim[slot] = MathUtils.approach(this.favoritesHoverAnim[slot], hovered ? 1f : 0f, HOVER_SPEED, dt);
            this.extractSwatchCell(graphics, cx0, y, swSize, favorites.get(i), this.favoritesHoverAnim[slot], favBase + i, alpha);
        }
    }

    public boolean handleFavoritesRowClick(final MouseButtonEvent event, final double mouseX, final double mouseY, final float x0, final float y, final float swSize) {
        final int button = event.button();
        if ((button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) || mouseY < y || mouseY >= y + swSize) {
            return false;
        }
        final List<Integer> favorites = WalksyLib.getInstance().getFavoriteColors().get();
        final int idx = swatchIndexAt(mouseX, x0, swSize, this.metrics.swatchGap() * this.uiScale);
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (idx >= 1 && idx - 1 < favorites.size()) {
                WalksyLib.getInstance().getFavoriteColors().remove(favorites.get(idx - 1));
            }
            return true;
        }
        if (idx == 0) {
            WalksyLib.getInstance().getFavoriteColors().add(this.color.get().getRGB() | 0xFF000000);
        } else if (idx > 0 && idx - 1 < favorites.size()) {
            this.onSwatch.accept(favorites.get(idx - 1));
        }
        return true;
    }
}
