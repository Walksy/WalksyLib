package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.BooleanOption;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.impl.WarningPopUp;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.ScreenGlobals;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;

import java.awt.*;

public class BooleanWidget extends OptionWidget {

    private final Option<Boolean> option;
    private final WarningPopUp warningPopUp;
    private final Animation toggleAnim;

    private int onX;
    private int offX;

    public BooleanWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<Boolean> option, final BooleanOption.Warning warning) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;

        this.onX = width - 6;
        this.offX = width - 15;

        final float initialPos = option.getValue() ? this.onX : this.offX;
        this.toggleAnim = new Animation(initialPos, 0.5f);

        if (warning != null) {
            this.warningPopUp = new WarningPopUp(screen, warning.title(), warning.message(),
                    () -> {
                        option.setValue(!option.getValue());
                        this.onChange();
                        if (warning.onYes() != null) warning.onYes().run();
                        this.screen.popUp.close();
                    },
                    () -> {
                        if (warning.onNo() != null) warning.onNo().run();
                        this.screen.popUp.close();
                    });
        } else {
            this.warningPopUp = null;
        }
    }

    @Override
    public void extract(final Graphics graphics, final int mouseX, final int mouseY, final float delta) {
        final GuiGraphicsExtractor extractor = graphics.extractor();
        if (ScreenGlobals.DEBUG) this.renderDebug(extractor);

        graphics.fillRoundedRect(this.getWidth() - 16, this.getY() + 3, 25, this.getHeight() - 6, 2, new Color(255, 255, 255, 20).getRGB());
        graphics.fillRoundedRectOutline(this.getWidth() - 16, this.getY() + 3, 25, this.getHeight() - 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());

        this.toggleAnim.update(delta);
        final int color = this.option.getValue() ? Color.WHITE.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
        final float animX = this.toggleAnim.getCurrentValue();

        graphics.fillRoundedRect(animX, this.getY() + 4, 14, this.getHeight() - 8, 2, color);
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        if (this.isHovered() && click.button() == 0) {
            if (this.warningPopUp != null && !this.warningPopUp.visible && !this.option.getValue()) {
                this.screen.popUp = this.warningPopUp;
                return;
            }

            this.option.setValue(!this.option.getValue());
            this.onChange();
        }
    }

    @Override
    public void onWidgetUpdate() {
        this.onX = this.width - 6;
        this.offX = this.width - 15;

        final float initialPos = this.option.getValue() ? this.onX : this.offX;
        this.toggleAnim.jumpTo(initialPos);
    }

    @Override
    public void playDownSound(final SoundManager soundManager) {}

    private void renderDebug(final GuiGraphicsExtractor extractor) {
        extractor.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), new Color(255, 255, 255, 150).getRGB());
    }

    @Override
    public void onChange() {
        super.onChange();
        this.toggleAnim.setTargetValue(this.option.getValue() ? this.onX : this.offX);
    }

    @Override
    public <V> void onThirdPartyChange(final V value) {
        super.onThirdPartyChange(value);
        this.toggleAnim.setTargetValue(this.option.getValue() ? this.onX : this.offX);
    }
}
