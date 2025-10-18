package main.walksy.lib.core.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public abstract class OptionWidget extends AbstractWidget {

    private final OptionGroup parent;
    private final Option<?> option;
    private boolean isHovered;

    public WalksyLibConfigScreen screen;
    public ButtonWidget resetButton;

    public boolean changesMade;
    public int mouseX, mouseY;

    public OptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, Option<?> option, int x, int y, int width, int height, String name) {
        super(x, y, width, height, Text.of(name));
        this.setPosition(x, y);
        this.parent = parent;
        this.option = option;
        this.screen = screen;
        this.changesMade = false;
        this.isHovered = false;

        int size = WalksyLibScreenManager.Globals.OPTION_HEIGHT;
        resetButton = new ButtonWidget(getX() + getWidth() - size + 15, getY(), size, size, false, Identifier.of("walksylib", "gui/widget/reset.png"), this::handleResetButtonClick, -1, 0);
        resetButton.setEnabled(option.hasChanged());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        isHovered = (mouseX >= getX() && mouseX < (getX() + getWidth())
                && mouseY >= getY() && mouseY < (getY() + getHeight()));


        if (isHovered)
        {
            screen.setFocusedOption(option);
        }

        context.enableScissor(0, 49, screen.width, screen.height - 28);

        if (!isAvailable()) {
            RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1f);
        }
        renderBase(context);


        resetButton.setEnabled(option.hasChanged() && isAvailable());
        resetButton.render(context, mouseX, mouseY, delta);
        context.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight() - 1);
        this.draw(context, isAvailable() ? mouseX : 0, isAvailable() ? mouseY : 0, delta);
        context.disableScissor();
        this.drawOutsideScissor(context, isAvailable() ? mouseX : 0, isAvailable() ? mouseY : 0, delta);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        context.disableScissor();
    }

    public void onMouseClick(double mouseX, double mouseY, int button) {}
    public void onMouseRelease(double mouseX, double mouseY, int button) {}
    public void onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {}
    public void onMouseMove(double mouseX, double mouseY) {}
    public void onMouseScroll(double mouseX, double mouseY, double verticalAmount) {}
    public void tick() {}
    public void onKeyPress(int keyCode, int scanCode, int modifiers) {}
    public void onCharTyped(char chr, int modifiers) {}
    public void onWidgetUpdate(int x, int y)
    {
        this.resetButton.setPosition(x, y);
        this.onWidgetUpdate();
    }

    public abstract void onWidgetUpdate();

    public abstract void draw(DrawContext context, int mouseX, int mouseY, float delta);

    public void drawOutsideScissor(DrawContext context, int mouseX, int mouseY, float delta) {}

    private void renderBase(DrawContext context)
    {
        Renderer2D.fillRoundedRectOutline(context, getX(), getY(), getWidth(), getHeight(), 2, 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer2D.fillRoundedRectOutline(context, getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        renderName(context);
    }

    public boolean isAvailable()
    {
        return option.isAvailable();
    }

    protected void renderName(DrawContext context)
    {
        context.drawTextWithShadow(screen.getTextRenderer(),
                option.getName(),
                getX() + 5,
                getTextYCentered() + 1,
                -1);
    }


    protected void handleResetButtonClick()
    {
        this.option.reset();
        this.onThirdPartyChange(this.option.getDefaultValue());
    }

    protected void renderHoverBackground(DrawContext context, int hoverLeft, int hoverRight) {
        if (isHovered()) {
            context.fill(hoverLeft, getY(), hoverRight, getY() + getHeight(), 0x64FFFFFF);
        }
    }


    protected int getTextYCentered() {
        int textHeight = screen.getTextRenderer().fontHeight;
        return getY() + (WalksyLibScreenManager.Globals.OPTION_HEIGHT - textHeight) / 2;
    }

    public boolean isHovered() {
        return isAvailable() && isHovered && isVisible();
    }

    public boolean isVisible() {
        return parent.isExpanded() && option.searched();
    }

    public boolean isInScissor(int scissorX, int scissorY, int scissorWidth, int scissorHeight) {
        return getX() + getWidth() > scissorX &&
                getX() < scissorX + scissorWidth &&
                getY() + getHeight() - 2 > scissorY &&
                getY() < scissorY + scissorHeight;
    }

    public void updateSearchQuery(String searchQuery) {
        this.option.updateSearchQ(searchQuery);
    }

    public void update()
    {
        screen.layoutGroupWidgets();
    }

    public void onChange()
    {
        screen.onChangesMade(option);
    }

    public <V> void onThirdPartyChange(V value)
    {
    }

    public OptionGroup getParent() {
        return parent;
    }

    public Option<?> getOption() {
        return option;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
