package main.walksy.lib.core.gui.widget.options.control;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.widget.AbstractTextEditWidget;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import net.minecraft.util.Mth;

public final class TextFieldControl {
    public static final float ROW_HEIGHT_RATIO = 0.72f;
    private static final float TEXT_INSET = 5f;
    private static final float TEXT_SIZE_RATIO = 0.55f;
    private static final float FIELD_CAP_HEIGHT = 0.7f;
    public static final int CURSOR_AWAY = WLScreen.POINTER_AWAY;
    private final Consumer<String> onChange;
    private final Supplier<String> placeholder;
    private final GlassStyle style;
    private final float uiScale;
    private final Field field = new Field();

    public TextFieldControl(final String initial, final Consumer<String> onChange, final String placeholder, final GlassStyle style, final float uiScale) {
        this(initial, onChange, placeholder == null ? null : () -> placeholder, style, uiScale);
    }

    public TextFieldControl(final String initial, final Consumer<String> onChange, final Supplier<String> placeholder, final GlassStyle style, final float uiScale) {
        this.onChange = onChange;
        this.placeholder = placeholder;
        this.style = style;
        this.uiScale = uiScale;
        this.field.setValue(initial == null ? "" : initial);
    }

    public String getText() {
        return this.field.getValue();
    }

    public void syncText(final String text) {
        final String incoming = text == null ? "" : text;
        if (!this.field.isFocused() && !this.field.getValue().equals(incoming)) {
            this.field.setValue(incoming);
        }
    }

    public void insertAtCaret(final String text) {
        if (text != null && !text.isEmpty()) {
            this.field.insert(text);
        }
    }

    public void setLayer(final int layer) {
        this.style.layer(layer);
    }

    public void setMaxLength(final int maxLength) {
        this.field.setMaxLength(maxLength);
    }

    public void extract(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final int mouseX, final int mouseY, final float delta, final float alpha, final boolean available, final boolean hoverEnabled) {
        this.field.setX(Math.round(x));
        this.field.setY(Math.round(y));
        this.field.setWidth(Math.max(1, Math.round(w)));
        this.field.setHeight(Math.max(1, Math.round(h)));
        this.field.active = available;
        this.field.setAlpha(alpha);
        final int mx = hoverEnabled ? mouseX : CURSOR_AWAY;
        final int my = hoverEnabled ? mouseY : CURSOR_AWAY;
        graphics.widget(this.field, mx, my, delta);
    }

    public boolean isOver(final double mouseX, final double mouseY) {
        return mouseX >= this.field.getX() && mouseX < this.field.getX() + this.field.getWidth() && mouseY >= this.field.getY() && mouseY < this.field.getY() + this.field.getHeight();
    }

    public boolean isFocused() {
        return this.field.isFocused();
    }

    public void setFocused(final boolean focused) {
        this.field.setFocused(focused);
    }

    public void clicked(final MouseButtonEvent event, final boolean doubleClick) {
        this.field.onClick(event, doubleClick);
    }

    public boolean dragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        return this.field.mouseDragged(event, dragX, dragY);
    }

    public boolean keyPressed(final KeyEvent event) {
        return this.field.keyPressed(event);
    }

    public boolean charTyped(final CharacterEvent event) {
        return this.field.charTyped(event);
    }

    private final class Field extends AbstractTextEditWidget {
        Field() {
            super(0, 0, 1, 1);
        }

        void insert(final String text) {
            this.insertText(text);
        }

        @Override
        protected void onTextChanged() {
            if (TextFieldControl.this.onChange != null) {
                TextFieldControl.this.onChange.accept(this.getValue());
            }
        }

        @Override
        protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
            final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f);
            if (alpha <= 0.001f) {
                return;
            }
            final float dt = WLRenderSystem.frameDeltaSeconds();
            final GlassStyle base = this.active ? TextFieldControl.this.style : GlassStyle.inactive(TextFieldControl.this.style);
            this.extractFieldGlass(graphics, base, alpha, TextFieldControl.this.uiScale, dt);
            final float inset = TEXT_INSET * TextFieldControl.this.uiScale;
            final float textStartX = this.getX() + inset;
            final float innerWidth = Math.max(4f, this.getWidth() - inset * 2f);
            final float size = Math.min(OptionRowWidget.LABEL_SIZE * TextFieldControl.this.uiScale, this.getHeight() * TEXT_SIZE_RATIO);
            final String visible = this.layoutText(textStartX, innerWidth, size);
            final float baseline = this.getY() + this.getHeight() / 2f + size * FIELD_CAP_HEIGHT * 0.5f;
            final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size);
            if (!visible.isEmpty()) {
                Fonts.SFPRO_DISPLAY.extract(graphics, visible, textStartX, textTop, size, ColorUtils.withAlpha(Theme.get().label(), alpha));
            } else if (TextFieldControl.this.placeholder != null && !this.isFocused()) {
                final String hint = TextFieldControl.this.placeholder.get();
                if (hint != null && !hint.isEmpty()) {
                    Fonts.SFPRO_DISPLAY.extract(graphics, hint, textStartX, textTop, size, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha));
                }
            }
            this.extractCaretAndSelection(graphics, "", textStartX, size, alpha, TextFieldControl.this.uiScale);
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            if (this.handleCommonKeyPress(event)) {
                return true;
            }
            if (!this.isFocused() || !this.active) {
                return false;
            }
            return switch (event.key()) {
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_ESCAPE -> {
                    this.setFocused(false);
                    yield true;
                }
                default -> false;
            };
        }
}
}
