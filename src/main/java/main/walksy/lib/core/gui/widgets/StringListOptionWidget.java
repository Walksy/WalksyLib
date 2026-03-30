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

    public StringListOptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<List<String>> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.setHeight();
        this.addButton = new ButtonWidget(getX() + width - 37, getY() + 3, 30, 14, false, "Add", this::onAdd);
    }

    @Override
    public void draw(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.addButton.extractWidgetRenderState(context, mouseX, mouseY, delta);
        context.horizontalLine(
                getX() + 1,
                getX() + getWidth() - 2,
                getY() + ScreenGlobals.OPTION_HEIGHT - 1,
                isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB()
        );
        if (this.option.getValue().isEmpty()) {
            int cX = this.getWidth() / 2;
            context.centeredText(
                    screen.getFont(),
                    "No Entries",
                    cX + screen.getFont().width("No Entries") / 2,
                    getTextYCentered() + 1 + this.ADDITIONAL_HEIGHT,
                    -1
            );
        }

        for (int i = 0; i < textboxes.size(); i++) {
            TextboxSubWidget textBox = textboxes.get(i);
            int width = screen.getFont().width(String.valueOf(i + 1));
            int off = -1;
            context.text(screen.getFont(), String.valueOf(i + 1), getX() + 5, textBox.getPos().y + 6 + off, -1, true);
            context.verticalLine(getX() + width + 8, textBox.getPos().y + 1 + off, textBox.getPos().y - 1 + off + textBox.getHeight(), (textBox.hovered || textBox.isFocused()) ? -1 : new Color(255, 255, 255, 180).getRGB());
            textBox.render(context, mouseX, mouseY, delta);
            removeButtons.get(i).extractWidgetRenderState(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void onMouseClick(MouseButtonEvent click, boolean doubled) {
        this.addButton.onClick(click, doubled);

        for (TextboxSubWidget textbox : textboxes) {
            textbox.setFocus(false);
            textbox.onClick(click, doubled);
        }

        for (int i = 0; i < removeButtons.size(); i++) {
            removeButtons.get(i).onClick(click, doubled);
        }

        if (pendingRemovalIndex >= 0) {
            List<String> mutable = new ArrayList<>(option.getValue());
            if (pendingRemovalIndex < mutable.size()) {
                mutable.remove(pendingRemovalIndex);
                option.setValue(mutable);
                setHeight();
            }
            pendingRemovalIndex = -1;
        }

        super.onMouseClick(click, doubled);
    }

    @Override
    public void onWidgetUpdate() {
        addButton.setPosition(getX() + width - 37, getY() + 3);

        for (int i = 0; i < textboxes.size(); i++) {
            int labelWidth = screen.getFont().width(String.valueOf(i + 1));
            textboxes.get(i).setPos(new Point(getX() + labelWidth + 10, getY() + 25 + i * 20));
            textboxes.get(i).setWidth(getWidth() - (labelWidth + 44));
            removeButtons.get(i).setPosition(getX() + getWidth() - 26, getY() + 25 + i * 20 + 1);
        }
    }

    @Override
    public void onKeyPress(KeyEvent input) {
        for (TextboxSubWidget textbox : textboxes) {
            textbox.onKeyPress(input);
        }
        super.onKeyPress(input);
    }

    @Override
    public void onCharTyped(CharacterEvent input) {
        for (TextboxSubWidget textbox : textboxes) {
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
        List<String> mutable = new ArrayList<>(option.getValue());
        mutable.add("");
        option.setValue(mutable);
        this.setHeight();
    }

    public void setHeight() {
        this.rebuildTextboxes();

        int size = this.option.getValue().size();
        this.ADDITIONAL_HEIGHT = size == 0 ? 20 : size * 20 + 4;

        this.setHeight(ScreenGlobals.OPTION_HEIGHT + ADDITIONAL_HEIGHT);
        this.update();
    }

    private void rebuildTextboxes() {
        textboxes.clear();
        removeButtons.clear();

        int yOffset = getY() + ScreenGlobals.OPTION_HEIGHT + 4;
        int textboxHeight = 18;
        List<String> currentValues = option.getValue();
        for (int i = 0; i < currentValues.size(); i++) {
            int y = yOffset + i * (textboxHeight + 2);
            int index = i;

            Consumer<String> onChange = newValue -> {
                List<String> newList = new ArrayList<>(option.getValue());
                newList.set(index, newValue);
                option.setValue(newList);
            };

            TextboxSubWidget textbox = new TextboxSubWidget(
                    screen,
                    getX() + 8,
                    y,
                    getWidth() - 34,
                    getWidth() - 34,
                    textboxHeight,
                    currentValues.get(i),
                    onChange,
                    false
            );

            ButtonWidget removeButton = new ButtonWidget(
                    getX() + getWidth() - 26,
                    y + 1,
                    20,
                    14,
                    false,
                    "-",
                    () -> pendingRemovalIndex = index
            );

            textboxes.add(textbox);
            removeButtons.add(removeButton);
        }
    }

    @Override
    public <V> void onThirdPartyChange(V value) {
        super.onThirdPartyChange(value);
        this.rebuildTextboxes();
        this.setHeight();
        this.onWidgetUpdate();
    }
}
