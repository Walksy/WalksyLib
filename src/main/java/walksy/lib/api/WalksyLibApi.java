package walksy.lib.api;

import net.minecraft.client.gui.screen.Screen;

import java.util.function.UnaryOperator;

public interface WalksyLibApi {
    UnaryOperator<Screen> getConfigScreen();
}
