package walksy.lib.core.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import walksy.lib.core.utils.RenderUtils;

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
            ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), new Color(0, 0, 0, 100).getRGB());
        }
        RenderUtils.fillRoundedRectOutline(ctx, getX(), getY(), width, height, 2, 1, new Color(255, 255, 255, hovered ? 100 : 51).getRGB());
        RenderUtils.fillRoundedRectOutline(ctx, getX() - 1, getY() - 1, width + 2, height + 2, 2, 1, new Color(0, 0, 0, 191).getRGB());

        String textString = getMessage().getString();
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(textString);
        int textHeight = MinecraftClient.getInstance().textRenderer.fontHeight;

        //why must I add one to each variable here??
        int textX = getX() + ((width - textWidth) / 2) + 1;
        int textY = getY() + ((height - textHeight) / 2) + 1;

        int color = hovered ? 0xFFCCCCCC : 0xFF888888;
        ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, textString, textX, textY, color);
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
}
