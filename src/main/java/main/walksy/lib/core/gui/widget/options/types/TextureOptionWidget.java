package main.walksy.lib.core.gui.widget.options.types;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.render.WLRenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.core.gui.animation.Materialize;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.api.render.AnimatedTextures;
import main.walksy.lib.api.render.CachedTextures;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import main.walksy.lib.core.platform.TinyFileDialogs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.options.RowContext;

public class TextureOptionWidget extends OptionRowWidget<Identifier> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x591710L);
    private static final float PREVIEW_CORNER_RATIO = 0.3f;
    private static final float HOVER_SPEED = 14f;
    private static final String EDIT_LABEL = "Edit Texture";
    private static final float EDIT_PILL_GAP = 8f;
    private static final float EDIT_PILL_H_PADDING = 8f;
    private static final float DESCRIPTION_SIZE = Type.FOOTNOTE.size();
    private final GlassStyle previewStyle;
    private final GlassStyle editPillStyle;
    private float previewX0;
    private float previewY0;
    private float previewSize;
    private float pillX0;
    private float pillY0;
    private float pillW;
    private float pillH;
    private static TextureOptionWidget dropTarget;
    private boolean popoverActive;
    private float hoverAnim;
    private boolean resolved;
    private Identifier resolvedIdentifier;
    private int resolvedWidth = 1;
    private int resolvedHeight = 1;
    private int contentX;
    private int contentY;
    private int contentW = 1;
    private int contentH = 1;
    private Identifier decodePendingId;

    public TextureOptionWidget(final Option<Identifier> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.previewStyle = ResetButton.knobStyle(ControlLayers.KNOB).fadeKey(FADE_KEYS.incrementAndGet());
        this.editPillStyle = ResetButton.knobStyle(ControlLayers.KNOB).fadeKey(FADE_KEYS.incrementAndGet());
    }

    private void resolve() {
        final Identifier id = this.option.getValue();
        if (this.resolved && Objects.equals(id, this.resolvedIdentifier)) {
            return;
        }
        if (Objects.equals(id, this.decodePendingId)) {
            return;
        }
        this.resolvedIdentifier = id;
        this.resolvedWidth = 1;
        this.resolvedHeight = 1;
        this.contentX = 0;
        this.contentY = 0;
        this.contentW = 1;
        this.contentH = 1;
        if (id == null) {
            this.resolved = true;
            return;
        }
        if (CachedTextures.isCacheBacked(id) && !AnimatedTextures.isRegistered(id)) {
            this.decodePendingId = id;
            CachedTextures.load(id, false, () -> {
                if (Objects.equals(this.decodePendingId, id)) {
                    this.decodePendingId = null;
                }
                this.resolved = false;
            });
            return;
        }
        this.resolved = true;
        this.updateResolvedDimensions(id);
    }

    private void updateResolvedDimensions(final Identifier id) {
        final AbstractTexture texture = Minecraft.getInstance()
            .getTextureManager()
            .getTexture(AnimatedTextures.currentFrame(id));
        if (texture == null) {
            return;
        }
        this.resolvedWidth = Math.max(1, texture.getTexture().getWidth(0));
        this.resolvedHeight = Math.max(1, texture.getTexture().getHeight(0));
        final int[] bounds = computeContentBounds(id, texture, this.resolvedWidth, this.resolvedHeight);
        this.contentX = bounds[0];
        this.contentY = bounds[1];
        this.contentW = bounds[2];
        this.contentH = bounds[3];
    }

    private static int[] computeContentBounds(final Identifier id, final AbstractTexture texture, final int fallbackW, final int fallbackH) {
        if (texture instanceof final DynamicTexture dynamic && dynamic.getPixels() != null) {
            return scanContentBounds(dynamic.getPixels(), fallbackW, fallbackH);
        }
        final var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isPresent()) {
            try (final InputStream stream = resource.get().open()) {
                final NativeImage image = NativeImage.read(stream);
                try {
                    return scanContentBounds(image, fallbackW, fallbackH);
                } finally {
                    image.close();
                }
            } catch (final IOException e) {
                WalksyLib.getLogger().err("Failed to read " + id + " for content bounds: " + e.getMessage());
            }
        }
        return new int[]{0, 0, fallbackW, fallbackH};
    }

    private static int[] scanContentBounds(final NativeImage image, final int fallbackW, final int fallbackH) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final int[] abgr = image.getPixelsABGR();
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            final int row = y * w;
            for (int x = 0; x < w; x++) {
                if ((abgr[row + x] >>> 24) != 0) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }
        if (maxX >= minX && maxY >= minY) {
            return new int[]{minX, minY, maxX - minX + 1, maxY - minY + 1};
        }
        return new int[]{0, 0, fallbackW, fallbackH};
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        this.resolve();
        final float previewSize = this.rowLabelHeight() * 0.8f;
        final float rowRight = this.getX() + this.getWidth() - PADDING * this.uiScale;
        final float pillTextSize = Type.FOOTNOTE.size() * this.uiScale;
        final float pillTextWidth = Fonts.SFPRO_DISPLAY.width(EDIT_LABEL, pillTextSize);
        final float pillW = pillTextWidth + EDIT_PILL_H_PADDING * 2f * this.uiScale;
        final float pillH = previewSize;
        final float pillX1 = rowRight;
        final float pillX0 = pillX1 - pillW;
        final float pillY0 = this.getY() + (this.rowLabelHeight() - pillH) / 2f;
        this.pillX0 = pillX0;
        this.pillY0 = pillY0;
        this.pillW = pillW;
        this.pillH = pillH;
        final float x1 = pillX0 - EDIT_PILL_GAP * this.uiScale;
        final float x0 = x1 - previewSize;
        final float y0 = this.getY() + (this.rowLabelHeight() - previewSize) / 2f;
        this.previewX0 = x0;
        this.previewY0 = y0;
        this.previewSize = previewSize;
        graphics.glass()
            .dimensions(x0, y0, previewSize, previewSize)
            .cornerRadius(previewSize * PREVIEW_CORNER_RATIO)
            .style(this.previewStyle.scaled(this.uiScale)
                .opacity(alpha))
            .extract();
        ResetButton.lift(false, () -> this.extractPreviewContent(graphics, x0, y0, previewSize, alpha));
        if (this.popoverActive) {
            this.hoverAnim = 0f;
            return;
        }
        final float swatchAlpha = alpha;
        final boolean hovered = available && this.isOverControl(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY);
        WLRenderSystem.cursors().requestControl(this.isOverControl(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY), available);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        final float hoverGlow = this.hoverAnim;
        graphics.glass()
            .dimensions(pillX0, pillY0, pillW, pillH)
            .cornerRadius(pillH / 2f)
            .style(this.editPillStyle.scaled(this.uiScale)
                .opacity(swatchAlpha))
            .hover(hoverGlow)
            .extract();
        ResetButton.lift(false, () -> this.extractPillLabel(graphics, pillX0, pillY0, pillW, pillH, swatchAlpha));
    }

    private void extractPreviewContent(final WLGraphics2D graphics, final float x0, final float y0, final float previewSize, final float alpha) {
        if (this.resolvedIdentifier != null) {
            extractFitted(graphics, AnimatedTextures.currentFrame(this.resolvedIdentifier), this.resolvedWidth, this.resolvedHeight, this.contentX, this.contentY, this.contentW, this.contentH, x0 + 3f * this.uiScale, y0 + 3f * this.uiScale, previewSize - 6f * this.uiScale, previewSize - 6f * this.uiScale, 2f * this.uiScale, alpha);
        } else {
            final float size = 8f * this.uiScale;
            final float capHeight = size * 0.59043f;
            final float baseline = y0 + previewSize / 2f + capHeight * 0.5f;
            final float top = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size);
            final String hint = "?";
            final float textWidth = Fonts.SFPRO_DISPLAY.width(hint, size);
            Fonts.SFPRO_DISPLAY.extract(graphics, hint, x0 + (previewSize - textWidth) / 2f, top, size, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha));
        }
    }

    private void extractPillLabel(final WLGraphics2D graphics, final float pillX0, final float pillY0, final float pillW, final float pillH, final float alpha) {
        final float pillTextSize = Type.FOOTNOTE.size() * this.uiScale;
        final float pillTextWidth = Fonts.SFPRO_DISPLAY.width(EDIT_LABEL, pillTextSize);
        final float pillCapHeight = pillTextSize * 0.59043f;
        final float pillBaseline = pillY0 + pillH / 2f + pillCapHeight * 0.5f;
        final float pillTextTop = Fonts.SFPRO_DISPLAY.topForBaseline(pillBaseline, pillTextSize);
        Fonts.SFPRO_DISPLAY.extract(graphics, EDIT_LABEL, pillX0 + (pillW - pillTextWidth) / 2f, pillTextTop, pillTextSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
    }

    private static void extractFitted(final WLGraphics2D graphics, final Identifier identifier, final int imgW, final int imgH, final int contentX, final int contentY, final int contentW, final int contentH, final float boxX, final float boxY, final float boxW, final float boxH, final float cornerRadius, final float alpha) {
        final float scale = Math.min(boxW / contentW, boxH / contentH);
        final float w = contentW * scale;
        final float h = contentH * scale;
        final float x = boxX + (boxW - w) / 2f;
        final float y = boxY + (boxH - h) / 2f;
        graphics.blitRounded(identifier, x, y, contentX, contentY, w, h, contentW, contentH, imgW, imgH, ColorUtils.withAlpha(0xFFFFFFFF, alpha), cornerRadius);
    }

    private boolean isOverControl(final double mouseX, final double mouseY) {
        final float slop = 2f * this.uiScale;
        return mouseX >= this.pillX0 - slop && mouseX <= this.pillX0 + this.pillW + slop && mouseY >= this.pillY0 - slop && mouseY <= this.pillY0 + this.pillH + slop;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive() || this.ctx == null || this.ctx.onOpenPopover() == null) {
            return false;
        }
        if (!this.isOverControl(event.x(), event.y())) {
            return false;
        }
        this.popoverActive = true;
        dropTarget = this;
        final LiquidRect anchor = new LiquidRect(this.pillX0, this.pillY0, this.pillW, this.pillH, this.pillH / 2f);
        this.ctx.onOpenPopover().accept(anchor, new TexturePickerPopover());
        return true;
    }

    public static void handleFileDrop(final List<Path> files) {
        final TextureOptionWidget target = dropTarget;
        if (target == null || !target.popoverActive || !target.isInteractive()) {
            return;
        }
        for (final Path file : files) {
            target.applyImageFile(file);
        }
    }

    private void openFileDialog() {
        TinyFileDialogs.openFile("Select an image", CachedTextures.IMAGE_FILTER_PATTERNS, "Image files", picked -> this.applyImageFile(Path.of(picked)));
    }

    private void applyImageFile(final Path file) {
        if (!this.isInteractive()) {
            return;
        }
        try {
            final Identifier identifier = CachedTextures.importFile(file);
            this.decodePendingId = identifier;
            this.resolved = false;
            this.resolvedIdentifier = identifier;
            this.resolvedWidth = 1;
            this.resolvedHeight = 1;
            this.contentX = 0;
            this.contentY = 0;
            this.contentW = 1;
            this.contentH = 1;
            CachedTextures.load(identifier, true, () -> {
                if (Objects.equals(this.decodePendingId, identifier)) {
                    this.decodePendingId = null;
                }
                this.resolved = false;
                this.option.setValue(identifier);
            });
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to load image " + file + ": " + e.getMessage());
        }
    }

    private final class TexturePickerPopover implements PopoverHost.PopoverContent {
        private static final int BASE_SECTIONS = 3;

        @Override
        public int preferredWidth() {
            return Math.round(200f * TextureOptionWidget.this.uiScale);
        }

        @Override
        public int preferredHeight() {
            final int base = Math.round(200f * TextureOptionWidget.this.uiScale);
            final List<String> lines = this.descriptionLines(TextureOptionWidget.this.uiScale, this.preferredWidth());
            if (lines.isEmpty()) {
                return base;
            }
            return base + Math.round(lines.size() * this.descriptionLineHeight(TextureOptionWidget.this.uiScale) + 6f * TextureOptionWidget.this.uiScale);
        }

        @Override
        public GlassStyle originStyle() {
            return TextureOptionWidget.this.editPillStyle.scaled(TextureOptionWidget.this.uiScale)
                .layer(PopoverHost.PANEL_LAYER);
        }

        private List<String> descriptionLines(final float uiScale, final float panelWidth) {
            final OptionDescription desc = TextureOptionWidget.this.option.getDescription();
            if (desc == null || desc.getType() != OptionDescription.Type.TEXT) {
                return List.of();
            }
            final String text = desc.getText().get();
            if (text == null || text.isEmpty()) {
                return List.of();
            }
            final float padding = 14f * uiScale;
            return Fonts.SFPRO_DISPLAY.wrap(text, DESCRIPTION_SIZE * uiScale, panelWidth - padding * 2f);
        }

        private float descriptionLineHeight(final float uiScale) {
            return DESCRIPTION_SIZE * uiScale * 1.3f;
        }

        @Override
        public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
            final float uiScale = TextureOptionWidget.this.uiScale;
            final float padding = 14f * uiScale;
            final float cx = panelX + panelW / 2f;
            final List<String> descLines = this.descriptionLines(uiScale, panelW);
            final int sections = BASE_SECTIONS + (descLines.isEmpty() ? 0 : 1);
            int section = 0;
            float a;
            final float titleSize = LABEL_SIZE * uiScale;
            final float capHeight = titleSize * 0.59043f;
            final float titleTop = Fonts.SFPRO_DISPLAY.topForBaseline(panelY + padding + capHeight, titleSize);
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, sections), cx, titleTop + titleSize / 2f, uiScale);
            Fonts.SFPRO_DISPLAY.extract(graphics, TextureOptionWidget.this.option.getName(), panelX + padding, titleTop, titleSize, ColorUtils.withAlpha(LABEL_COLOR(), a));
            Materialize.exit(graphics.pose());
            float contentTop = titleTop + titleSize + 6f * uiScale;
            if (!descLines.isEmpty()) {
                final float descSize = DESCRIPTION_SIZE * uiScale;
                final float descLineHeight = this.descriptionLineHeight(uiScale);
                final float descCy = contentTop + (descLines.size() * descLineHeight) / 2f;
                a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, sections), cx, descCy, uiScale);
                for (int i = 0; i < descLines.size(); i++) {
                    final float lineTop = Fonts.SFPRO_DISPLAY.topForBaseline(contentTop + i * descLineHeight + descSize * 0.59043f, descSize);
                    Fonts.SFPRO_DISPLAY.extract(graphics, descLines.get(i), panelX + padding, lineTop, descSize, ColorUtils.withAlpha(Theme.get().label(), a));
                }
                Materialize.exit(graphics.pose());
                contentTop += descLines.size() * descLineHeight;
            }
            final String hint = "Drop an image anywhere here, or click to browse";
            final float hintSize = titleSize * 0.85f;
            final float hintWidth = Fonts.SFPRO_DISPLAY.width(hint, hintSize);
            final float hintBaseline = panelY + panelH - padding;
            final float hintTop = Fonts.SFPRO_DISPLAY.topForBaseline(hintBaseline, hintSize);
            final float previewY0 = contentTop + 10f * uiScale;
            final float previewY1 = hintTop - 10f * uiScale;
            final float previewX0 = panelX + padding;
            final float previewX1 = panelX + panelW - padding;
            final float previewCy = (previewY0 + previewY1) / 2f;
            TextureOptionWidget.this.resolve();
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, sections), cx, previewCy, uiScale);
            if (TextureOptionWidget.this.resolvedIdentifier != null) {
                extractFitted(graphics, AnimatedTextures.currentFrame(TextureOptionWidget.this.resolvedIdentifier), TextureOptionWidget.this.resolvedWidth, TextureOptionWidget.this.resolvedHeight, TextureOptionWidget.this.contentX, TextureOptionWidget.this.contentY, TextureOptionWidget.this.contentW, TextureOptionWidget.this.contentH, previewX0, previewY0, previewX1 - previewX0, previewY1 - previewY0, 6f * uiScale, a);
            } else {
                final String empty = "No image set";
                final float emptySize = hintSize;
                final float emptyWidth = Fonts.SFPRO_DISPLAY.width(empty, emptySize);
                final float emptyBaseline = previewY0 + (previewY1 - previewY0) / 2f + emptySize * 0.59043f * 0.5f;
                Fonts.SFPRO_DISPLAY.extract(graphics, empty, panelX + (panelW - emptyWidth) / 2f, Fonts.SFPRO_DISPLAY.topForBaseline(emptyBaseline, emptySize), emptySize, ColorUtils.withAlpha(Theme.get().label(), a));
            }
            Materialize.exit(graphics.pose());
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, sections), cx, hintTop + hintSize / 2f, uiScale);
            Fonts.SFPRO_DISPLAY.extract(graphics, hint, panelX + (panelW - hintWidth) / 2f, hintTop, hintSize, ColorUtils.withAlpha(Theme.get().label(), a));
            Materialize.exit(graphics.pose());
        }

        @Override
        public void onClosed() {
            TextureOptionWidget.this.popoverActive = false;
            if (dropTarget == TextureOptionWidget.this) {
                dropTarget = null;
            }
        }

        @Override
        public boolean hasTriggerHandoff() {
            return true;
        }

        @Override
        public void extractTriggerHandoff(final WLGraphics2D graphics, final float handoff) {
            TextureOptionWidget.this.extractPillLabel(graphics, TextureOptionWidget.this.pillX0, TextureOptionWidget.this.pillY0, TextureOptionWidget.this.pillW, TextureOptionWidget.this.pillH, handoff);
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
            TextureOptionWidget.this.openFileDialog();
            return true;
        }
}
}
