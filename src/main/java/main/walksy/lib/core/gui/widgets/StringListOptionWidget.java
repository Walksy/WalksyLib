package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.sub.TextboxSubWidget;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.ScreenGlobals;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StringListOptionWidget extends OptionWidget {

    private final Option<List<String>> option;
    private final ButtonWidget addButton;
    private final List<TextboxSubWidget> textboxes = new ArrayList<>();
    private final List<ButtonWidget> removeButtons = new ArrayList<>();
    private int pendingRemovalIndex = -1;
    public int ADDITIONAL_HEIGHT;

    public StringListOptionWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<List<String>> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.setHeight();
        this.addButton = new ButtonWidget(this.getX() + width - 37, this.getY() + 3, 30, 14, false, "Add", this::onAdd);
    }

    @Override
    public void draw(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {
        this.addButton.extractRenderState(context, mouseX, mouseY, delta);
        context.horizontalLine(
                this.getX() + 1,
                this.getX() + this.getWidth() - 2,
                this.getY() + ScreenGlobals.OPTION_HEIGHT - 1,
                this.isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB()
        );
        if (this.option.getValue().isEmpty()) {
            final int cX = this.getWidth() / 2;
            context.centeredText(
                    this.screen.getFont(),
                    "No Entries",
                    cX + this.screen.getFont().width("No Entries") / 2,
                    this.getTextYCentered() + 1 + this.ADDITIONAL_HEIGHT,
                    -1
            );
        }

        for (int i = 0; i < this.textboxes.size(); i++) {
            final TextboxSubWidget textBox = this.textboxes.get(i);
            final int width = this.screen.getFont().width(String.valueOf(i + 1));
            final int off = -1;
            context.text(this.screen.getFont(), String.valueOf(i + 1), this.getX() + 5, textBox.getPos().y + 6 + off, -1, true);
            context.verticalLine(this.getX() + width + 8, textBox.getPos().y + 1 + off, textBox.getPos().y - 1 + off + textBox.getHeight(), (textBox.hovered || textBox.isFocused()) ? -1 : new Color(255, 255, 255, 180).getRGB());
            textBox.render(context, mouseX, mouseY, delta);
            this.removeButtons.get(i).extractRenderState(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        this.addButton.onClick(click, doubled);

        for (final TextboxSubWidget textbox : this.textboxes) {
            textbox.setFocus(false);
            textbox.onClick(click, doubled);
        }

        for (int i = 0; i < this.removeButtons.size(); i++) {
            this.removeButtons.get(i).onClick(click, doubled);
        }

        if (this.pendingRemovalIndex >= 0) {
            final List<String> mutable = new ArrayList<>(this.option.getValue());
            if (this.pendingRemovalIndex < mutable.size()) {
                mutable.remove(this.pendingRemovalIndex);
                this.option.setValue(mutable);
                this.setHeight();
            }
            this.pendingRemovalIndex = -1;
        }

        super.onMouseClick(click, doubled);
    }

    @Override
    public void onWidgetUpdate() {
        this.addButton.setPosition(this.getX() + this.width - 37, this.getY() + 3);

        for (int i = 0; i < this.textboxes.size(); i++) {
            final int labelWidth = this.screen.getFont().width(String.valueOf(i + 1));
            this.textboxes.get(i).setPos(new Point(this.getX() + labelWidth + 10, this.getY() + 25 + i * 20));
            this.textboxes.get(i).setWidth(this.getWidth() - (labelWidth + 44));
            this.removeButtons.get(i).setPosition(this.getX() + this.getWidth() - 26, this.getY() + 25 + i * 20 + 1);
        }
    }

    @Override
    public void onKeyPress(final KeyEvent input) {
        for (final TextboxSubWidget textbox : this.textboxes) {
            textbox.onKeyPress(input);
        }
        super.onKeyPress(input);
    }

    @Override
    public void onCharTyped(final CharacterEvent input) {
        for (final TextboxSubWidget textbox : this.textboxes) {
            textbox.onCharTyped(input);
        }
        super.onCharTyped(input);
    }

    @Override
    protected void handleResetButtonClick() {
        super.handleResetButtonClick();
        this.setHeight();
    }

    @Override
    public boolean isHovered() {
        return false;
    }

    private void onAdd() {
        final List<String> mutable = new ArrayList<>(this.option.getValue());
        mutable.add("");
        this.option.setValue(mutable);
        this.setHeight();
    }

    public void setHeight() {
        this.rebuildTextboxes();

        final int size = this.option.getValue().size();
        this.ADDITIONAL_HEIGHT = size == 0 ? 20 : size * 20 + 4;

        this.setHeight(ScreenGlobals.OPTION_HEIGHT + this.ADDITIONAL_HEIGHT);
        this.update();
    }

    private void rebuildTextboxes() {
        this.textboxes.clear();
        this.removeButtons.clear();

        final int yOffset = this.getY() + ScreenGlobals.OPTION_HEIGHT + 4;
        final int textboxHeight = 18;
        final List<String> currentValues = this.option.getValue();
        for (int i = 0; i < currentValues.size(); i++) {
            final int y = yOffset + i * (textboxHeight + 2);
            final int index = i;

            final Consumer<String> onChange = newValue -> {
                final List<String> newList = new ArrayList<>(this.option.getValue());
                newList.set(index, newValue);
                this.option.setValue(newList);
            };

            final TextboxSubWidget textbox = new TextboxSubWidget(
                    this.screen,
                    this.getX() + 8,
                    y,
                    this.getWidth() - 34,
                    this.getWidth() - 34,
                    textboxHeight,
                    currentValues.get(i),
                    onChange,
                    false
            );

            final ButtonWidget removeButton = new ButtonWidget(
                    this.getX() + this.getWidth() - 26,
                    y + 1,
                    20,
                    14,
                    false,
                    "-",
                    () -> this.pendingRemovalIndex = index
            );

            this.textboxes.add(textbox);
            this.removeButtons.add(removeButton);
        }
    }

    @Override
    public <V> void onThirdPartyChange(final V value) {
        super.onThirdPartyChange(value);
        this.rebuildTextboxes();
        this.setHeight();
        this.onWidgetUpdate();
    }
}
