package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Renderer;

import java.awt.*;

public class ButtonWidget extends ClickableWidget {

    private Runnable action;
    private final Identifier texture;
    private final PixelGrid grid;
    private final boolean background;
    public boolean overrideHover = false;
    public boolean hovered = true;
    public float scrollY = 0;
    private int outlineColor = -1, hoveredColor = -1;
    private boolean h = false;

    public ButtonWidget(int x, int y, int width, int height, boolean background, String name, @Nullable Runnable action) {
        super(x, y, width, height, Text.of(name));
        this.action = action;
        this.background = background;
        this.grid = null;
        this.texture = null;
    }

    public ButtonWidget(int x, int y, int width, int height, boolean background, Identifier texture, @Nullable Runnable action) {
        super(x, y, width, height, Text.of(texture.getPath()));
        this.action = action;
        this.background = background;
        this.grid = null;
        this.texture = texture;
    }


    public void setOutlineColor(int color, int hovered)
    {
        this.outlineColor = color;
        this.hoveredColor = hovered;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int drawX = getX();
        int drawY = getY() - (int) scrollY;
        h = ctx.scissorContains(mouseX, mouseY) && mouseX >= this.getX() && mouseY >= drawY && mouseX < this.getX() + this.width && mouseY < drawY + this.height;
        if (background) {
            ctx.fill(drawX, drawY, drawX + getWidth(), drawY + getHeight(),
                    this.active ? new Color(0, 0, 0, 100).getRGB() : new Color(50, 50, 50, 100).getRGB());
        }

        if (this.outlineColor == -1) {
            Renderer.fillRoundedRectOutline(ctx, drawX, drawY, width, height, 2, 1,
                    this.active
                            ? new Color(255, 255, 255, (isHovered() || overrideHover)
                            ? MainColors.OUTLINE_WHITE_HOVERED.getAlpha()
                            : MainColors.OUTLINE_WHITE.getAlpha()).getRGB()
                            : new Color(180, 180, 180, 50).getRGB());
        } else {
            Renderer.fillRoundedRectOutline(ctx, drawX, drawY, width, height, 2, 1,
                    this.active
                            ? (isHovered() || overrideHover ? hoveredColor : outlineColor)
                            : new Color(180, 180, 180, 50).getRGB());
        }

        Renderer.fillRoundedRectOutline(ctx, drawX - 1, drawY - 1, width + 2, height + 2, 2, 1,
                this.active ? new Color(0, 0, 0, 191).getRGB() : new Color(30, 30, 30, 120).getRGB());

        if (texture == null && grid == null) {
            String text = getMessage().getString();
            int textX = drawX + ((width - MinecraftClient.getInstance().textRenderer.getWidth(text)) / 2) + 1;
            int textY = drawY + ((height - MinecraftClient.getInstance().textRenderer.fontHeight) / 2) + 1;

            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, text.equals("-") ? textX - 1 : textX, textY,
                    this.active ? ((isHovered() || overrideHover) ? 0xFFCCCCCC : 0xFF888888) : 0xFF555555);
        } else if (grid == null) {
            ctx.drawTexture(RenderLayer::getGuiTextured, texture, drawX + 3, drawY + 3, 0, 0, 16, 16, 16, 16, 16, 16, this.active ? -1 : Color.GRAY.getRGB());
        }

        if (grid != null) {
            WalksyLib.getInstance().get2DRenderer().renderGridTexture(ctx, grid, drawX + 3, drawY + 3, 1, 1, 1);
        }
    }



    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        if (isHovered() && action != null) {
            action.run();
        }
    }

    @Override
    public boolean isHovered() {
        return this.h && hovered;
    }

    public void setListener(Runnable runnable)
    {
        this.action = runnable;
    }

    public void setEnabled(boolean bl)
    {
        this.active = bl;
    }
}
