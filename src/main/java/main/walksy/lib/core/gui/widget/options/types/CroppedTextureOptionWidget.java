package main.walksy.lib.core.gui.widget.options.types;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.value.CroppedTexture;
import main.walksy.lib.core.gui.animation.Materialize;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.platform.TinyFileDialogs;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.api.render.AnimatedTextures;
import main.walksy.lib.api.render.CachedTextures;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class CroppedTextureOptionWidget extends OptionRowWidget<CroppedTexture> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0xC20770L);
    private static final float PREVIEW_CORNER_RATIO = 0.3f;
    private static final float HOVER_SPEED = 14f;
    private static final String EDIT_LABEL = "Edit Texture";
    private static final float EDIT_PILL_GAP = 8f;
    private static final float EDIT_PILL_H_PADDING = 8f;
    private static CroppedTextureOptionWidget dropTarget;
    private final GlassStyle previewStyle;
    private final GlassStyle editPillStyle;
    private float pillX0;
    private float pillY0;
    private float pillW;
    private float pillH;
    private boolean popoverActive;
    private float hoverAnim;

    public CroppedTextureOptionWidget(final Option<CroppedTexture> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.previewStyle = ResetButton.knobStyle(ControlLayers.KNOB).fadeKey(FADE_KEYS.incrementAndGet());
        this.editPillStyle = ResetButton.knobStyle(ControlLayers.KNOB).fadeKey(FADE_KEYS.incrementAndGet());
    }

    public static void handleFileDrop(final List<Path> files) {
        final CroppedTextureOptionWidget target = dropTarget;
        if (target == null || !target.popoverActive || !target.isInteractive() || files.isEmpty()) {
            return;
        }
        target.applyImageFile(files.get(0));
    }

    private float aspect() {
        return this.option.getCropAspect();
    }

    private CroppedTexture value() {
        final CroppedTexture value = this.option.getValue();
        return value == null ? CroppedTexture.NONE : value;
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final float previewH = this.rowLabelHeight() * 0.8f;
        final float previewW = this.aspect() > 0f ? previewH * Math.min(1f, this.aspect()) : previewH;
        final float rowRight = this.getX() + this.getWidth() - PADDING * this.uiScale;
        final float pillTextSize = Type.FOOTNOTE.size() * this.uiScale;
        this.pillW = Fonts.SFPRO_DISPLAY.width(EDIT_LABEL, pillTextSize) + EDIT_PILL_H_PADDING * 2f * this.uiScale;
        this.pillH = previewH;
        this.pillX0 = rowRight - this.pillW;
        this.pillY0 = this.getY() + (this.rowLabelHeight() - this.pillH) / 2f;
        final float x0 = this.pillX0 - EDIT_PILL_GAP * this.uiScale - previewW;
        final float y0 = this.getY() + (this.rowLabelHeight() - previewH) / 2f;
        graphics.glass()
            .dimensions(x0, y0, previewW, previewH)
            .cornerRadius(Math.min(previewW, previewH) * PREVIEW_CORNER_RATIO)
            .style(this.previewStyle.scaled(this.uiScale)
                .opacity(alpha))
            .extract();
        ResetButton.lift(false, () -> this.extractPreviewContent(graphics, x0, y0, previewW, previewH, alpha));
        if (this.popoverActive) {
            this.hoverAnim = 0f;
            return;
        }
        final boolean overPill = this.isOverControl(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY);
        WLRenderSystem.cursors().requestControl(overPill, available);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, available && overPill ? 1f : 0f, HOVER_SPEED, dt);
        graphics.glass()
            .dimensions(this.pillX0, this.pillY0, this.pillW, this.pillH)
            .cornerRadius(this.pillH / 2f)
            .style(this.editPillStyle.scaled(this.uiScale)
                .opacity(alpha))
            .hover(this.hoverAnim)
            .extract();
        ResetButton.lift(false, () -> this.extractPillLabel(graphics, alpha));
    }

    private void extractPreviewContent(final WLGraphics2D graphics, final float x0, final float y0, final float w, final float h, final float alpha) {
        final CroppedTexture value = this.value();
        final float inset = 3f * this.uiScale;
        if (!value.isEmpty() && CachedTextures.isReady(value.texture())) {
            graphics.blitUv(AnimatedTextures.currentFrame(value.texture()), x0 + inset, y0 + inset, w - inset * 2f, h - inset * 2f, value.u0(), value.v0(), value.u1(), value.v1(), ColorUtils.withAlpha(0xFFFFFFFF, alpha));
            return;
        }
        if (!value.isEmpty()) {
            CachedTextures.ensureLoaded(value.texture());
        }
        final float size = 8f * this.uiScale;
        final String hint = "?";
        final float top = Fonts.SFPRO_DISPLAY.topForBaseline(y0 + h / 2f + size * 0.59043f * 0.5f, size);
        Fonts.SFPRO_DISPLAY.extract(graphics, hint, x0 + (w - Fonts.SFPRO_DISPLAY.width(hint, size)) / 2f, top, size, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha));
    }

    private void extractPillLabel(final WLGraphics2D graphics, final float alpha) {
        final float size = Type.FOOTNOTE.size() * this.uiScale;
        final float top = Fonts.SFPRO_DISPLAY.topForBaseline(this.pillY0 + this.pillH / 2f + size * 0.59043f * 0.5f, size);
        Fonts.SFPRO_DISPLAY.extract(graphics, EDIT_LABEL, this.pillX0 + (this.pillW - Fonts.SFPRO_DISPLAY.width(EDIT_LABEL, size)) / 2f, top, size, ColorUtils.withAlpha(Theme.get().label(), alpha));
    }

    private boolean isOverControl(final double mouseX, final double mouseY) {
        final float slop = 2f * this.uiScale;
        return mouseX >= this.pillX0 - slop && mouseX <= this.pillX0 + this.pillW + slop && mouseY >= this.pillY0 - slop && mouseY <= this.pillY0 + this.pillH + slop;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive() || !this.isOverControl(event.x(), event.y())) {
            return false;
        }
        final LiquidRect anchor = new LiquidRect(this.pillX0, this.pillY0, this.pillW, this.pillH, this.pillH / 2f);
        if (!this.openPopover(anchor, new CropEditorPopover())) {
            return false;
        }
        this.popoverActive = true;
        dropTarget = this;
        return true;
    }

    private void openFileDialog() {
        TinyFileDialogs.openFile("Select an image", CachedTextures.IMAGE_FILTER_PATTERNS, "Image files", picked -> this.applyImageFile(Path.of(picked)));
    }

    private void applyImageFile(final Path file) {
        if (!this.isInteractive()) {
            return;
        }
        final Identifier identifier;
        try {
            identifier = CachedTextures.importFile(file);
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to load image " + file + ": " + e.getMessage());
            return;
        }
        CachedTextures.load(identifier, true, () -> {
            final int[] size = CachedTextures.size(identifier);
            this.option.setValue(size == null ? CroppedTexture.of(identifier) : CroppedTexture.centered(identifier, size[0], size[1], this.aspect()));
        });
    }

    private final class CropEditorPopover implements PopoverHost.PopoverContent {
        private static final int SECTIONS = 4;
        private static final float WIDTH = 280f;
        private static final float HEIGHT = 340f;
        private static final float TITLE_SIZE = LABEL_SIZE;
        private static final float HINT_SIZE = Type.FOOTNOTE.size();
        private static final float BUTTON_H = 20f;
        private static final float GAP = 8f;
        private static final float HANDLE_SIZE = 7f;
        private static final float HANDLE_REACH = 9f;
        private static final float OUTLINE = 1.5f;
        private static final float DIM_ALPHA = 0.3f;
        private static final float ZOOM_STEP = 0.1f;
        private static final float MIN_CROP_FRACTION = 0.05f;
        private ResetButton chooseButton;
        private ResetButton resetButton;
        private ResetButton removeButton;
        private float imageX;
        private float imageY;
        private float imageScale = 1f;
        private int imageW;
        private int imageH;
        private DragMode drag = DragMode.NONE;
        private int dragCorner;
        private float grabX;
        private float grabY;
        private float anchorX;
        private float anchorY;
        private float cropX;
        private float cropY;
        private float cropW;
        private float cropH;

        private enum DragMode {
            NONE,
            MOVE,
            RESIZE
        }

        private void ensureButtons(final int width, final int height) {
            if (this.chooseButton != null && this.chooseButton.getWidth() == width && this.chooseButton.getHeight() == height) {
                return;
            }
            this.chooseButton = new ResetButton(0, 0, width, height, "Choose Image", CroppedTextureOptionWidget.this::openFileDialog, PopoverHost.CONTENT_LAYER);
            this.resetButton = new ResetButton(0, 0, width, height, "Reset Crop", this::resetCrop, PopoverHost.CONTENT_LAYER);
            this.removeButton = new ResetButton(0, 0, width, height, "Remove", () -> CroppedTextureOptionWidget.this.option.setValue(CroppedTexture.NONE), PopoverHost.CONTENT_LAYER);
        }

        private List<ResetButton> buttons() {
            return this.chooseButton == null ? List.of() : List.of(this.chooseButton, this.resetButton, this.removeButton);
        }

        private float s() {
            return CroppedTextureOptionWidget.this.uiScale;
        }

        @Override
        public int preferredWidth() {
            return Math.round(WIDTH * this.s());
        }

        @Override
        public int preferredHeight() {
            return Math.round(HEIGHT * this.s());
        }

        @Override
        public GlassStyle originStyle() {
            return CroppedTextureOptionWidget.this.editPillStyle.scaled(this.s())
                .layer(PopoverHost.PANEL_LAYER);
        }

        @Override
        public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
            final float s = this.s();
            final float pad = 14f * s;
            final float cx = panelX + panelW / 2f;
            int section = 0;
            final float titleSize = TITLE_SIZE * s;
            final float titleTop = Fonts.SFPRO_DISPLAY.topForBaseline(panelY + pad + titleSize * 0.59043f, titleSize);
            float a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, titleTop + titleSize / 2f, s);
            Fonts.SFPRO_DISPLAY.extract(graphics, CroppedTextureOptionWidget.this.option.getName(), panelX + pad, titleTop, titleSize, ColorUtils.withAlpha(LABEL_COLOR(), a));
            Materialize.exit(graphics.pose());
            final float buttonH = BUTTON_H * s;
            final float buttonY = panelY + panelH - pad - buttonH;
            final float hintSize = HINT_SIZE * s;
            final float hintTop = buttonY - GAP * s - hintSize;
            final float editorTop = titleTop + titleSize + GAP * s;
            final float editorBottom = hintTop - GAP * s;
            final float editorX0 = panelX + pad;
            final float editorX1 = panelX + panelW - pad;
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, (editorTop + editorBottom) / 2f, s);
            this.extractEditor(graphics, editorX0, editorTop, editorX1 - editorX0, editorBottom - editorTop, mouseX, mouseY, a);
            Materialize.exit(graphics.pose());
            final String hint = this.hasImage() ? "Drag to move, drag a corner to resize, scroll to zoom" : "Drop an image here, or choose one below";
            final String fitted = Fonts.SFPRO_DISPLAY.truncate(hint, hintSize, panelW - pad * 2f);
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, hintTop + hintSize / 2f, s);
            Fonts.SFPRO_DISPLAY.extract(graphics, fitted, cx - Fonts.SFPRO_DISPLAY.width(fitted, hintSize) / 2f, hintTop, hintSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), a));
            Materialize.exit(graphics.pose());
            final float buttonW = (panelW - pad * 2f - GAP * s * 2f) / 3f;
            final float buttonAlpha = Materialize.stagger(reveal, section, SECTIONS);
            this.ensureButtons(Math.round(buttonW), Math.round(buttonH));
            this.place(this.chooseButton, editorX0, buttonY, buttonAlpha);
            this.place(this.resetButton, editorX0 + buttonW + GAP * s, buttonY, buttonAlpha);
            this.place(this.removeButton, editorX0 + (buttonW + GAP * s) * 2f, buttonY, buttonAlpha);
            this.resetButton.active = this.hasImage();
            this.removeButton.active = !CroppedTextureOptionWidget.this.value().isEmpty();
            graphics.widget(this.chooseButton, mouseX, mouseY, delta);
            graphics.widget(this.resetButton, mouseX, mouseY, delta);
            graphics.widget(this.removeButton, mouseX, mouseY, delta);
        }

        private void place(final ResetButton button, final float x, final float y, final float alpha) {
            button.setX(Math.round(x));
            button.setY(Math.round(y));
            button.setAlpha(alpha);
        }

        private boolean hasImage() {
            final CroppedTexture value = CroppedTextureOptionWidget.this.value();
            return !value.isEmpty() && CachedTextures.isReady(value.texture());
        }

        private void extractEditor(final WLGraphics2D graphics, final float boxX, final float boxY, final float boxW, final float boxH, final int mouseX, final int mouseY, final float alpha) {
            final CroppedTexture value = CroppedTextureOptionWidget.this.value();
            final int[] size = value.isEmpty() ? null : CachedTextures.size(value.texture());
            if (size == null) {
                if (!value.isEmpty()) {
                    CachedTextures.ensureLoaded(value.texture());
                }
                this.imageW = 0;
                this.imageH = 0;
                graphics.fillRoundedGradient(boxX, boxY, boxX + boxW, boxY + boxH, this.emptyColor(alpha), this.emptyColor(alpha), this.emptyColor(alpha), this.emptyColor(alpha), 6f * this.s());
                return;
            }
            this.imageW = size[0];
            this.imageH = size[1];
            this.imageScale = Math.min(boxW / this.imageW, boxH / this.imageH);
            final float drawW = this.imageW * this.imageScale;
            final float drawH = this.imageH * this.imageScale;
            this.imageX = boxX + (boxW - drawW) / 2f;
            this.imageY = boxY + (boxH - drawH) / 2f;
            if (this.drag == DragMode.NONE) {
                this.cropX = value.u0() * this.imageW;
                this.cropY = value.v0() * this.imageH;
                this.cropW = (value.u1() - value.u0()) * this.imageW;
                this.cropH = (value.v1() - value.v0()) * this.imageH;
            }
            final Identifier frame = AnimatedTextures.currentFrame(value.texture());
            graphics.blitUv(frame, this.imageX, this.imageY, drawW, drawH, 0f, 0f, 1f, 1f, ColorUtils.withAlpha(0xFFFFFFFF, alpha * DIM_ALPHA));
            final float sx0 = this.toScreenX(this.cropX);
            final float sy0 = this.toScreenY(this.cropY);
            final float sx1 = this.toScreenX(this.cropX + this.cropW);
            final float sy1 = this.toScreenY(this.cropY + this.cropH);
            graphics.blitUv(frame, sx0, sy0, sx1 - sx0, sy1 - sy0, this.cropX / this.imageW, this.cropY / this.imageH, (this.cropX + this.cropW) / this.imageW, (this.cropY + this.cropH) / this.imageH, ColorUtils.withAlpha(0xFFFFFFFF, alpha));
            final int accent = ColorUtils.withAlpha(Theme.get().accent(), alpha);
            final float t = OUTLINE * this.s();
            graphics.fill(sx0 - t, sy0 - t, sx1 + t, sy0, accent, null);
            graphics.fill(sx0 - t, sy1, sx1 + t, sy1 + t, accent, null);
            graphics.fill(sx0 - t, sy0, sx0, sy1, accent, null);
            graphics.fill(sx1, sy0, sx1 + t, sy1, accent, null);
            final float half = HANDLE_SIZE * this.s() / 2f;
            final int handle = ColorUtils.withAlpha(0xFFFFFFFF, alpha);
            for (int corner = 0; corner < 4; corner++) {
                final float hx = (corner & 1) == 0 ? sx0 : sx1;
                final float hy = (corner & 2) == 0 ? sy0 : sy1;
                graphics.fill(hx - half - t, hy - half - t, hx + half + t, hy + half + t, accent, null);
                graphics.fill(hx - half, hy - half, hx + half, hy + half, handle, null);
            }
            if (this.drag == DragMode.MOVE || (this.drag == DragMode.NONE && this.cornerAt(mouseX, mouseY) < 0 && this.insideCrop(mouseX, mouseY))) {
                WLRenderSystem.cursors().request(Cursors.Cursor.MOVE);
            } else if (this.drag == DragMode.RESIZE || this.cornerAt(mouseX, mouseY) >= 0) {
                WLRenderSystem.cursors().request(Cursors.Cursor.POINTER);
            }
        }

        private int emptyColor(final float alpha) {
            return ColorUtils.withAlpha(Theme.get().label(), alpha * 0.06f);
        }

        private float toScreenX(final float imageX) {
            return this.imageX + imageX * this.imageScale;
        }

        private float toScreenY(final float imageY) {
            return this.imageY + imageY * this.imageScale;
        }

        private float toImageX(final double screenX) {
            return (float) ((screenX - this.imageX) / this.imageScale);
        }

        private float toImageY(final double screenY) {
            return (float) ((screenY - this.imageY) / this.imageScale);
        }

        private boolean insideCrop(final double mouseX, final double mouseY) {
            final float x = this.toImageX(mouseX);
            final float y = this.toImageY(mouseY);
            return x >= this.cropX && x <= this.cropX + this.cropW && y >= this.cropY && y <= this.cropY + this.cropH;
        }

        private boolean insideImage(final double mouseX, final double mouseY) {
            final float x = this.toImageX(mouseX);
            final float y = this.toImageY(mouseY);
            return x >= 0f && x <= this.imageW && y >= 0f && y <= this.imageH;
        }

        private int cornerAt(final double mouseX, final double mouseY) {
            if (this.imageW <= 0) {
                return -1;
            }
            final float reach = HANDLE_REACH * this.s();
            for (int corner = 0; corner < 4; corner++) {
                final float hx = this.toScreenX((corner & 1) == 0 ? this.cropX : this.cropX + this.cropW);
                final float hy = this.toScreenY((corner & 2) == 0 ? this.cropY : this.cropY + this.cropH);
                if (Math.abs(mouseX - hx) <= reach && Math.abs(mouseY - hy) <= reach) {
                    return corner;
                }
            }
            return -1;
        }

        private float maxCropWidth() {
            final float aspect = CroppedTextureOptionWidget.this.aspect();
            return aspect > 0f ? Math.min(this.imageW, this.imageH * aspect) : this.imageW;
        }

        private float heightFor(final float width, final float fallback) {
            final float aspect = CroppedTextureOptionWidget.this.aspect();
            return aspect > 0f ? width / aspect : fallback;
        }

        private float minCropWidth() {
            return Math.max(1f, this.maxCropWidth() * MIN_CROP_FRACTION);
        }

        private void clampPosition() {
            this.cropX = Mth.clamp(this.cropX, 0f, Math.max(0f, this.imageW - this.cropW));
            this.cropY = Mth.clamp(this.cropY, 0f, Math.max(0f, this.imageH - this.cropH));
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
            for (final ResetButton button : this.buttons()) {
                if (button.active && button.isMouseOver(event.x(), event.y())) {
                    return button.mouseClicked(event, doubleClick);
                }
            }
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.hasImage() || this.imageW <= 0) {
                return true;
            }
            final int corner = this.cornerAt(event.x(), event.y());
            if (corner >= 0) {
                this.drag = DragMode.RESIZE;
                this.dragCorner = corner;
                this.anchorX = (corner & 1) == 0 ? this.cropX + this.cropW : this.cropX;
                this.anchorY = (corner & 2) == 0 ? this.cropY + this.cropH : this.cropY;
                return true;
            }
            if (!this.insideImage(event.x(), event.y())) {
                return true;
            }
            if (!this.insideCrop(event.x(), event.y())) {
                this.cropX = this.toImageX(event.x()) - this.cropW / 2f;
                this.cropY = this.toImageY(event.y()) - this.cropH / 2f;
                this.clampPosition();
            }
            this.drag = DragMode.MOVE;
            this.grabX = this.toImageX(event.x()) - this.cropX;
            this.grabY = this.toImageY(event.y()) - this.cropY;
            return true;
        }

        @Override
        public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (this.drag == DragMode.MOVE) {
                this.cropX = this.toImageX(event.x()) - this.grabX;
                this.cropY = this.toImageY(event.y()) - this.grabY;
                this.clampPosition();
                return true;
            }
            if (this.drag == DragMode.RESIZE) {
                this.resizeTo(this.toImageX(event.x()), this.toImageY(event.y()));
                return true;
            }
            return false;
        }

        private void resizeTo(final float pointerX, final float pointerY) {
            final boolean right = (this.dragCorner & 1) != 0;
            final boolean down = (this.dragCorner & 2) != 0;
            final float roomX = right ? this.imageW - this.anchorX : this.anchorX;
            final float roomY = down ? this.imageH - this.anchorY : this.anchorY;
            final float wantedW = right ? pointerX - this.anchorX : this.anchorX - pointerX;
            final float wantedH = down ? pointerY - this.anchorY : this.anchorY - pointerY;
            final float aspect = CroppedTextureOptionWidget.this.aspect();
            final float width;
            final float height;
            if (aspect > 0f) {
                final float limit = Math.min(roomX, roomY * aspect);
                width = Mth.clamp(Math.max(wantedW, wantedH * aspect), Math.min(this.minCropWidth(), limit), limit);
                height = width / aspect;
            } else {
                width = Mth.clamp(wantedW, Math.min(this.minCropWidth(), roomX), roomX);
                height = Mth.clamp(wantedH, Math.min(Math.max(1f, this.imageH * MIN_CROP_FRACTION), roomY), roomY);
            }
            this.cropW = width;
            this.cropH = height;
            this.cropX = right ? this.anchorX : this.anchorX - width;
            this.cropY = down ? this.anchorY : this.anchorY - height;
        }

        @Override
        public boolean mouseReleased(final MouseButtonEvent event) {
            boolean handled = false;
            for (final ResetButton button : this.buttons()) {
                handled |= button.mouseReleased(event);
            }
            if (this.drag != DragMode.NONE) {
                this.drag = DragMode.NONE;
                this.commit();
                return true;
            }
            return handled;
        }

        @Override
        public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (scrollY == 0 || !this.hasImage() || this.imageW <= 0 || this.drag != DragMode.NONE || !this.insideImage(mouseX, mouseY)) {
                return false;
            }
            final float centerX = this.cropX + this.cropW / 2f;
            final float centerY = this.cropY + this.cropH / 2f;
            final float factor = scrollY > 0 ? 1f - ZOOM_STEP : 1f + ZOOM_STEP;
            final float width = Mth.clamp(this.cropW * factor, this.minCropWidth(), this.maxCropWidth());
            this.cropH = Math.min(this.heightFor(width, Mth.clamp(this.cropH * factor, 1f, this.imageH)), this.imageH);
            this.cropW = width;
            this.cropX = centerX - this.cropW / 2f;
            this.cropY = centerY - this.cropH / 2f;
            this.clampPosition();
            this.commit();
            return true;
        }

        private void commit() {
            if (this.imageW <= 0) {
                return;
            }
            final CroppedTexture value = CroppedTextureOptionWidget.this.value();
            CroppedTextureOptionWidget.this.option.setValue(value.withCrop(this.cropX / this.imageW, this.cropY / this.imageH, (this.cropX + this.cropW) / this.imageW, (this.cropY + this.cropH) / this.imageH));
        }

        private void resetCrop() {
            final CroppedTexture value = CroppedTextureOptionWidget.this.value();
            final int[] size = CachedTextures.size(value.texture());
            if (size != null) {
                CroppedTextureOptionWidget.this.option.setValue(CroppedTexture.centered(value.texture(), size[0], size[1], CroppedTextureOptionWidget.this.aspect()));
            }
        }

        @Override
        public void onClosed() {
            this.drag = DragMode.NONE;
            CroppedTextureOptionWidget.this.popoverActive = false;
            if (dropTarget == CroppedTextureOptionWidget.this) {
                dropTarget = null;
            }
        }

        @Override
        public boolean hasTriggerHandoff() {
            return true;
        }

        @Override
        public void extractTriggerHandoff(final WLGraphics2D graphics, final float handoff) {
            CroppedTextureOptionWidget.this.extractPillLabel(graphics, handoff);
        }
    }
}
