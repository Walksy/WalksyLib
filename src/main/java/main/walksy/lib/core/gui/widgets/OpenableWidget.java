package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import net.minecraft.client.gui.DrawContext;

public abstract class OpenableWidget extends OptionWidget {

    public boolean open = false;
    public int OPEN_HEIGHT;

    public OpenableWidget(OptionGroup parent, WalksyLibConfigScreen screen, Option<?> option, int x, int y, int width, int height, String name, int openedHeight) {
        super(parent, screen, option, x, y, width, height, name);
        this.OPEN_HEIGHT = openedHeight;
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
    }


    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        if (isHovered() && button == 0) {
            toggleOpen();
        }
    }

    private void toggleOpen() {
        boolean prev = open;
        open = !open;
        if (open) {
            this.setHeight(OPEN_HEIGHT);
        } else {
            this.setHeight(WalksyLibScreenManager.Globals.OPTION_HEIGHT);
        }
        this.onOpen(prev);
        this.update();
    }

    protected abstract void onOpen(boolean prevValue);

    @Override
    public void onWidgetUpdate() {

    }
}
