package main.walksy.lib.core.gui.widgets;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

public abstract class AbstractWidget extends ClickableWidget {

    public AbstractWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }
}
