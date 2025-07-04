package walksy.lib.core.gui.widgets;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import walksy.lib.core.config.impl.Option;

public class BooleanWidget extends OptionWidget {

    private Option<Boolean> option;

    public BooleanWidget(int x, int y, int width, int height, Option<Boolean> option) {
        super(x, y, width, height, option.getName());
        this.option = option;
    }


    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
