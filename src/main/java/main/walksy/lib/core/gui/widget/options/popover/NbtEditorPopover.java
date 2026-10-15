package main.walksy.lib.core.gui.widget.options.popover;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.gui.style.PopoverStyle;
import main.walksy.lib.api.config.value.ItemSpec;
import main.walksy.lib.core.gui.animation.Materialize;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.MultilineTextEditor;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;

public final class NbtEditorPopover implements PopoverHost.PopoverContent {
    public static final float WIDTH = 300f;
    public static final float HEIGHT = 260f;
    public static final float MIN_WIDTH = 170f;
    private static final float PAD = 14f;
    private static final float BUTTON_H = 16f;
    private static final float BUTTON_GAP = 6f;
    private static final float STATUS_H = 22f;
    private static final float TITLE_GAP = 8f;
    private static final float TEXT_SIZE = 7.5f;
    private static final int SECTIONS = 3;
    private final Supplier<ItemSpec> spec;
    private final Consumer<Consumer<ItemSpec>> edit;
    private final Runnable requestClose;
    private final Supplier<GlassStyle> originStyle;
    private final float uiScale;
    private final int panelLayer;
    private final MultilineTextEditor editor;
    private final WLButton applyButton;
    private final WLButton resetButton;
    private String status;
    private boolean statusIsError;
    private final String original;
    private BiConsumer<WLGraphics2D, Float> triggerHandoff;
    private int widthLimit = Integer.MAX_VALUE;

    public NbtEditorPopover(final Supplier<ItemSpec> spec, final Consumer<Consumer<ItemSpec>> edit, final Runnable requestClose, final Supplier<GlassStyle> originStyle, final float uiScale, final int panelLayer, final long fadeKeyBase) {
        this.spec = spec;
        this.edit = edit;
        this.requestClose = requestClose;
        this.originStyle = originStyle;
        this.uiScale = uiScale;
        this.panelLayer = panelLayer;
        final String snbt = spec.get() == null ? null : spec.get().toSnbt();
        this.original = snbt == null ? "" : snbt;
        this.editor = new MultilineTextEditor(this.original);
        this.editor.setFocused(true);
        if (snbt == null) {
            this.status = "NBT editing is unavailable right now";
            this.statusIsError = true;
        }
        this.applyButton = new WLButton(0, 0, 1, 1, "Apply", b -> this.apply());
        this.resetButton = new WLButton(0, 0, 1, 1, "Revert", b -> {
            this.editor.setText(this.original);
            this.status = null;
            this.statusIsError = false;
        });
        int i = 0;
        for (final WLButton button : new WLButton[]{this.applyButton, this.resetButton}) {
            button.style()
                .layer(panelLayer + 2)
                .fadeKey(fadeKeyBase + i++)
                .refractionThickness(ItemEditorPopover.SMALL_BUTTON_REFRACTION);
        }
    }

    public NbtEditorPopover withTriggerHandoff(final BiConsumer<WLGraphics2D, Float> drawer) {
        this.triggerHandoff = drawer;
        return this;
    }

    public NbtEditorPopover limitWidth(final int maxWidth) {
        this.widthLimit = Math.max(Math.round(MIN_WIDTH * this.s()), maxWidth);
        return this;
    }

    @Override
    public boolean hasTriggerHandoff() {
        return this.triggerHandoff != null;
    }

    @Override
    public void extractTriggerHandoff(final WLGraphics2D graphics, final float handoff) {
        if (this.triggerHandoff != null) {
            this.triggerHandoff.accept(graphics, handoff);
        }
    }

    private void apply() {
        final ItemSpec target = this.spec.get();
        if (target == null) {
            this.status = "The slot is empty";
            this.statusIsError = true;
            return;
        }
        final ItemSpec probe = target.copy();
        final String error = probe.applySnbt(this.editor.getText());
        if (error != null) {
            this.status = error;
            this.statusIsError = true;
            return;
        }
        this.edit.accept(item -> item.applySnbt(this.editor.getText()));
        this.status = null;
        if (this.requestClose != null) {
            this.requestClose.run();
        }
    }

    private float s() {
        return this.uiScale;
    }

    @Override
    public int preferredWidth() {
        return Math.min(Math.round(WIDTH * this.s()), this.widthLimit);
    }

    @Override
    public int preferredHeight() {
        return Math.round(HEIGHT * this.s());
    }

    private PopoverStyle style;

    public NbtEditorPopover style(final PopoverStyle style) {
        this.style = style;
        return this;
    }

    @Override
    public PopoverStyle style() {
        return this.style == null ? PopoverStyle.getDefault() : this.style;
    }

    @Override
    public GlassStyle originStyle() {
        return this.originStyle == null ? null : this.originStyle.get();
    }

    @Override
    public boolean bridged() {
        return false;
    }

    @Override
    public float lensScale() {
        return 0.6f;
    }

    private float titleTop(final int panelY) {
        final float size = OptionRowWidget.LABEL_SIZE * this.s();
        return Fonts.SFPRO_DISPLAY.topForBaseline(panelY + PAD * this.s() + size * 0.59043f, size);
    }

    private float editorTop(final int panelY) {
        return this.titleTop(panelY) + OptionRowWidget.LABEL_SIZE * this.s() + TITLE_GAP * this.s();
    }

    private float editorBottom(final int panelY, final int panelH) {
        return panelY + panelH - PAD * this.s() - BUTTON_H * this.s() - BUTTON_GAP * this.s() - STATUS_H * this.s();
    }

    private float statusTop(final int panelY, final int panelH) {
        return this.editorBottom(panelY, panelH) + 4f * this.s();
    }

    private float buttonsTop(final int panelY, final int panelH) {
        return panelY + panelH - PAD * this.s() - BUTTON_H * this.s();
    }

    @Override
    public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
        final float s = this.s();
        final var pose = graphics.pose();
        final float cx = panelX + panelW / 2f;
        final float x0 = panelX + PAD * s;
        final float x1 = panelX + panelW - PAD * s;
        final float titleSize = OptionRowWidget.LABEL_SIZE * s;
        final float titleTop = this.titleTop(panelY);
        float a = Materialize.enter(pose, Materialize.stagger(reveal, 0, SECTIONS), cx, titleTop + titleSize / 2f, s);
        Fonts.SFPRO_DISPLAY_BOLD.extract(graphics, "NBT", x0, titleTop, titleSize, ColorUtils.withAlpha(OptionRowWidget.LABEL_COLOR(), a));
        final ItemSpec item = this.spec.get();
        if (item != null) {
            final float subSize = Type.FOOTNOTE.size() * s;
            final float titleW = Fonts.SFPRO_DISPLAY_BOLD.width("NBT", titleSize);
            final String subtitle = Fonts.SFPRO_DISPLAY.truncate(item.itemDisplayName(), subSize, Math.max(0f, x1 - x0 - titleW - 8f * s));
            final float subW = Fonts.SFPRO_DISPLAY.width(subtitle, subSize);
            Fonts.SFPRO_DISPLAY.extract(graphics, subtitle, x1 - subW, Fonts.SFPRO_DISPLAY.topForBaseline(titleTop + titleSize * 0.8f, subSize), subSize, ColorUtils.withAlpha(this.style().tertiaryLabel(), a));
        }
        Materialize.exit(pose);
        final float top = this.editorTop(panelY);
        final float bottom = this.editorBottom(panelY, panelH);
        a = Materialize.enter(pose, Materialize.stagger(reveal, 1, SECTIONS), cx, (top + bottom) / 2f, s);
        this.editor.extract(graphics, x0, top, x1 - x0, Math.max(10f, bottom - top), mouseX, mouseY, delta / 20f, a, TEXT_SIZE * s, s);
        Materialize.exit(pose);
        a = Materialize.enter(pose, Materialize.stagger(reveal, 2, SECTIONS), cx, this.buttonsTop(panelY, panelH) + BUTTON_H * s / 2f, s);
        this.extractStatus(graphics, x0, x1, this.statusTop(panelY, panelH), a);
        this.extractButtons(graphics, x0, x1, this.buttonsTop(panelY, panelH), mouseX, mouseY, delta, a);
        Materialize.exit(pose);
    }

    private void extractStatus(final WLGraphics2D graphics, final float x0, final float x1, final float y, final float alpha) {
        final float s = this.s();
        final float size = Type.FOOTNOTE.size() * s;
        final String text = this.status != null ? this.status : "Edit and press Apply.";
        final int color = this.status == null ? this.style()
            .tertiaryLabel() : this.statusIsError ? this.style()
            .destructive() : this.style()
            .accent();
        final float lineHeight = size * 1.25f;
        int line = 0;
        for (final String wrapped : Fonts.SFPRO_DISPLAY.wrap(text, size, x1 - x0)) {
            if (line >= 2) {
                break;
            }
            Fonts.SFPRO_DISPLAY.extract(graphics, wrapped, x0, Fonts.SFPRO_DISPLAY.topForBaseline(y + size + line * lineHeight, size), size, ColorUtils.withAlpha(color, alpha));
            line++;
        }
    }

    private void extractButtons(final WLGraphics2D graphics, final float x0, final float x1, final float y, final int mouseX, final int mouseY, final float delta, final float alpha) {
        final float s = this.s();
        final float h = BUTTON_H * s;
        final float gap = BUTTON_GAP * s;
        final float half = (x1 - x0 - gap) / 2f;
        this.layout(this.resetButton, x0, y, half, h, alpha);
        this.layout(this.applyButton, x0 + half + gap, y, half, h, alpha);
        this.resetButton.active = !this.editor.getText().equals(this.original);
        graphics.widget(this.resetButton, mouseX, mouseY, delta);
        graphics.widget(this.applyButton, mouseX, mouseY, delta);
    }

    private void layout(final WLButton button, final float x, final float y, final float w, final float h, final float alpha) {
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.max(1, Math.round(w)));
        button.setHeight(Math.max(1, Math.round(h)));
        button.setAlpha(alpha);
        button.setCornerRadius(Math.min(w, h) * 0.35f);
        button.setTextSize(Math.max(Type.CAPTION2.size() * this.s(), h * 0.46f));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (this.applyButton.isMouseOver(event.x(), event.y())) {
            this.editor.setFocused(false);
            this.applyButton.onClick(event, doubleClick);
            return true;
        }
        if (this.resetButton.active && this.resetButton.isMouseOver(event.x(), event.y())) {
            this.editor.setFocused(false);
            this.resetButton.onClick(event, doubleClick);
            return true;
        }
        if (this.editor.isOver(event.x(), event.y())) {
            this.editor.mouseClicked(event, doubleClick);
            return true;
        }
        this.editor.setFocused(false);
        return true;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY, final int panelX, final int panelY, final int panelW, final int panelH) {
        return this.editor.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        this.applyButton.onRelease(event);
        this.resetButton.onRelease(event);
        this.editor.mouseReleased();
        return false;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY, final int panelX, final int panelY, final int panelW, final int panelH) {
        return this.editor.mouseScrolled(scrollY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.editor.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.editor.charTyped(event);
    }
}
