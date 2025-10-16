package main.walksy.lib.api;

import net.minecraft.client.gui.screen.Screen;

import java.util.function.UnaryOperator;
//TODO Fix animations (like in the boolean widget) not being reset when I click undo/reset etc
public interface WalksyLibApi {
    UnaryOperator<Screen> getConfigScreen();
}
