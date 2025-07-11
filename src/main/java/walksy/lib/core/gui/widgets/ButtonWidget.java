package walksy.lib.core.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import walksy.lib.core.utils.MainColors;
import walksy.lib.core.utils.Renderer;

import java.awt.*;

public class ButtonWidget extends ClickableWidget {

    private final Runnable action;
    private final boolean background;

    public ButtonWidget(int x, int y, int width, int height, boolean background, String name, @Nullable Runnable action) {
        super(x, y, width, height, Text.of(name));
        this.action = action;
        this.background = background;
    }


    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (background) {
            ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                    this.active ? new Color(0, 0, 0, 100).getRGB() : new Color(50, 50, 50, 100).getRGB());
        }

        Renderer.fillRoundedRectOutline(ctx, getX(), getY(), width, height, 2, 1,
                this.active ? new Color(255, 255, 255, hovered ? MainColors.OUTLINE_WHITE_HOVERED.getAlpha() : MainColors.OUTLINE_WHITE.getAlpha()).getRGB() : new Color(180, 180, 180, 50).getRGB());

        Renderer.fillRoundedRectOutline(ctx, getX() - 1, getY() - 1, width + 2, height + 2, 2, 1,
                this.active ? new Color(0, 0, 0, 191).getRGB() : new Color(30, 30, 30, 120).getRGB());

        String text = getMessage().getString();
        int textX = getX() + ((width - MinecraftClient.getInstance().textRenderer.getWidth(text)) / 2) + 1;
        int textY = getY() + ((height - MinecraftClient.getInstance().textRenderer.fontHeight) / 2) + 1;

        ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, textX, textY,
                this.active ? (hovered ? 0xFFCCCCCC : 0xFF888888) : 0xFF555555);

    }


    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        if (hovered && action != null) {
            action.run();
        }
    }

    public void setEnabled(boolean bl)
    {
        this.active = bl;
    }
}
