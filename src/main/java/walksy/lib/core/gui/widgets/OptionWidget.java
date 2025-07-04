package walksy.lib.core.gui.widgets;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public abstract class OptionWidget extends ClickableWidget {

    public OptionWidget(int x, int y, int width, int height, String name) {
        super(x, y, width, height, Text.of(name));
    }
}
