package main.walksy.lib.core.gui.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class BaseScreen extends Screen {

    private final Screen parent;

    protected BaseScreen(String title, Screen parent) {
        super(Text.of(title));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void close() {
        super.close();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
