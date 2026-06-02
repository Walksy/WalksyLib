package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class ButtonWidget extends AbstractWidget {

    private Runnable action;
    private final Identifier texture;
    private final PixelGrid grid;
    private final boolean background;
    public boolean overrideHover = false;
    public boolean hovered = true;
    public float scrollY = 0;
    private int outlineColor = -1, hoveredColor = -1;
    private boolean h = false;
    private int textureOffsetX = 1, textureOffsetY = 1;

    public ButtonWidget(final int x, final int y, final int width, final int height, final boolean background, final String name, @Nullable final Runnable action) {
        super(x, y, width, height, Component.literal(name));
        this.action = action;
        this.background = background;
        this.grid = null;
        this.texture = null;
    }

    public ButtonWidget(final int x, final int y, final int width, final int height, final boolean background, final Identifier texture, @Nullable final Runnable action) {
        super(x, y, width, height, Component.literal(texture.getPath()));
        this.action = action;
        this.background = background;
        this.grid = null;
        this.texture = texture;
    }

    public ButtonWidget(final int x, final int y, final int width, final int height, final boolean background, final Identifier texture, @Nullable final Runnable action, final int offsetX, final int offsetY) {
        super(x, y, width, height, Component.literal(texture.getPath()));
        this.action = action;
        this.background = background;
        this.grid = null;
        this.texture = texture;
        this.textureOffsetX = offsetX;
        this.textureOffsetY = offsetY;
    }


    public void setOutlineColor(final int color, final int hovered) {
        this.outlineColor = color;
        this.hoveredColor = hovered;
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor ctx, final int mouseX, final int mouseY, final float delta) {
        final int drawX = this.getX();
        final int drawY = this.getY() - (int) this.scrollY;
        this.h = ctx.containsPointInScissor(mouseX, mouseY) && mouseX >= this.getX() && mouseY >= drawY && mouseX < this.getX() + this.width && mouseY < drawY + this.height;
        if (this.background) {
            ctx.fill(drawX, drawY, drawX + this.getWidth(), drawY + this.getHeight(),
                    this.active ? new Color(0, 0, 0, 100).getRGB() : new Color(50, 50, 50, 100).getRGB());
        }

        if (this.outlineColor == -1) {
            new Graphics(ctx).fillRoundedRectOutline(drawX, drawY, this.width, this.height, 2, 1,
                    this.active
                            ? new Color(255, 255, 255, (this.isHovered() || this.overrideHover)
                            ? MainColors.OUTLINE_WHITE_HOVERED.getAlpha()
                            : MainColors.OUTLINE_WHITE.getAlpha()).getRGB()
                            : new Color(180, 180, 180, 50).getRGB());
        } else {
            new Graphics(ctx).fillRoundedRectOutline(drawX, drawY, this.width, this.height, 2, 1,
                    this.active
                            ? (this.isHovered() || this.overrideHover ? this.hoveredColor : this.outlineColor)
                            : new Color(180, 180, 180, 50).getRGB());
        }

        new Graphics(ctx).fillRoundedRectOutline(drawX - 1, drawY - 1, this.width + 2, this.height + 2, 2, 1,
                this.active ? new Color(0, 0, 0, 191).getRGB() : new Color(30, 30, 30, 120).getRGB());

        if (this.texture == null && this.grid == null) {
            final String text = this.getMessage().getString();
            final int textX = drawX + ((this.width - Minecraft.getInstance().font.width(text)) / 2) + 1;
            final int textY = drawY + ((this.height - Minecraft.getInstance().font.lineHeight) / 2) + 1;

            ctx.text(Minecraft.getInstance().font, text, text.equals("-") ? textX - 1 : textX, textY,
                    this.active ? ((this.isHovered() || this.overrideHover) ? 0xFFCCCCCC : 0xFF888888) : 0xFF555555);
        } else if (this.grid == null) {
            ctx.blit(RenderPipelines.GUI_TEXTURED, this.texture, drawX + 3 + this.textureOffsetX, drawY + 3 + this.textureOffsetY, 0, 0, 16, 16, 16, 16, 16, 16, this.active ? -1 : Color.GRAY.getRGB());
        }

        if (this.grid != null) {
            new Graphics(ctx).renderGridTexture(this.grid, drawX + 3, drawY + 3, 1, 1, false);
        }
    }

    @Override
    public void onClick(@NonNull final MouseButtonEvent click, final boolean doubled) {
        super.onClick(click, doubled);
        if (this.isHovered()) {
            if (this.action != null) {
                this.action.run();
            }
        }
    }

    @Override
    public boolean isHovered() {
        return this.h && this.hovered;
    }

    @Override
    protected void updateWidgetNarration(@NonNull final NarrationElementOutput output) {}

    public void setListener(final Runnable runnable) {
        this.action = runnable;
    }

    public void setEnabled(final boolean bl) {
        this.active = bl;
    }
}
