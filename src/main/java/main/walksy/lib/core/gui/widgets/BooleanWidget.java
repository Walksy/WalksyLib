package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.BooleanOption;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.impl.WarningPopUp;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;

import java.awt.*;

public class BooleanWidget extends OptionWidget {

    private final Option<Boolean> option;
    private final WarningPopUp warningPopUp;
    private final Animation toggleAnim;

    private int onX;
    private int offX;

    public BooleanWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<Boolean> option, BooleanOption.Warning warning) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;

        this.onX = width - 6;
        this.offX = width - 15;

        float initialPos = option.getValue() ? onX : offX;
        this.toggleAnim = new Animation(initialPos, 0.5f);

        if (warning != null) {
            this.warningPopUp = new WarningPopUp(screen, warning.title, warning.message,
                    () -> {
                        option.setValue(!option.getValue());
                        onChange();
                        if (warning.onYes != null) warning.onYes.run();
                        this.screen.popUp.close();
                    },
                    () -> {
                        if (warning.onNo != null) warning.onNo.run();
                        this.screen.popUp.close();
                    });
        } else {
            this.warningPopUp = null;
        }
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        if (WalksyLibScreenManager.Globals.DEBUG) renderDebug(context);

        Renderer2D.fillRoundedRect(context, getWidth() - 16, getY() + 3, 25, getHeight() - 6, 2, new Color(255, 255, 255, 20).getRGB());
        Renderer2D.fillRoundedRectOutline(context, getWidth() - 16, getY() + 3, 25, getHeight() - 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());

        toggleAnim.update(delta);
        int color = option.getValue() ? Color.WHITE.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
        float animX = toggleAnim.getCurrentValue();

        Renderer2D.fillRoundedRect(context, animX, getY() + 4, 14, getHeight() - 8, 2, color);
    }

    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        if (isHovered() && button == 0) {
            ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());

            if (this.warningPopUp != null && !this.warningPopUp.visible && !this.option.getValue()) {
                this.screen.popUp = this.warningPopUp;
                return;
            }

            option.setValue(!option.getValue());
            onChange();
        }
    }

    @Override
    public void onWidgetUpdate()
    {
        this.onX = width - 6;
        this.offX = width - 15;

        float initialPos = option.getValue() ? onX : offX;
        this.toggleAnim.jumpTo(initialPos);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {}

    private void renderDebug(DrawContext context) {
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), new Color(255, 255, 255, 150).getRGB());
    }

    @Override
    public void onChange() {
        super.onChange();
        toggleAnim.setTargetValue(option.getValue() ? onX : offX);
    }

    @Override
    public <V> void onThirdPartyChange(V value) {
        super.onThirdPartyChange(value);
        toggleAnim.setTargetValue(option.getValue() ? onX : offX);
    }
}
