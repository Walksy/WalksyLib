package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.impl.FrameManagerPopUp;
import main.walksy.lib.core.gui.popup.impl.GridEditorPopUp;
import main.walksy.lib.core.gui.widgets.sub.SliderSubWidget;
import main.walksy.lib.core.gui.widgets.sub.adaptor.IntSliderAdapter;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Scroller;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PixelGridAnimationWidget extends OpenableWidget {

    public final ButtonWidget editHudButton;
    public final ButtonWidget editFrameButton;
    public final ButtonWidget viewFrames;
    private final Option<PixelGridAnimation> option;
    private final SliderSubWidget<Integer> animationSpeedSlider;
    private final List<ButtonWidget> buttonFrames = new ArrayList<>();
    private PixelGrid viewingGrid;
    private final Scroller scroller;
    private int frameToReplace = -1;

    public PixelGridAnimationWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<PixelGridAnimation> option) {
        super(parent, screen, option, x, y, width, height, option.getName(), (WalksyLibScreenManager.Globals.OPTION_HEIGHT * 6));
        this.option = option;
        this.scroller = new Scroller(0, 2);
        this.editHudButton = new ButtonWidget(getWidth() - 82, getY() + 3, 50, 14, false, "Edit Hud", () -> this.handleEditHudButtonClick(screen));
        this.editFrameButton = new ButtonWidget(getWidth() - 142, getY() + 101, 60, 14, false, "Edit Frame", this::handleEditFrameButtonClick);
        this.viewFrames = new ButtonWidget(getX() + 70, getY() + 101, 77, 14, false, "View Frames", this::handleViewFramesButtonClick);
        this.setupFrames(-1);

        List<PixelGrid> frames = option.getValue().getFrames();
        if (!frames.isEmpty()) {
            this.viewingGrid = frames.get(0).copy();
        }
        this.animationSpeedSlider = new SliderSubWidget<>(getX() + 75, getY() + 38, 100, WalksyLibScreenManager.Globals.OPTION_HEIGHT - 12, new IntSliderAdapter(0, 20, option.getValue().getAnimationSpeed()), option.getValue().getAnimationSpeed(), option.getValue()::setAnimationSpeed, true);
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
        this.animationSpeedSlider.setOnChange(option.getValue()::setAnimationSpeed);
        if (this.option.has2DPosition()) {
            this.editHudButton.render(context, mouseX, mouseY, delta);
        }

        context.drawVerticalLine(
                getX() + getWidth() - 38,
                getY(),
                getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1,
                isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB()
        );

        if (open) {
            this.viewFrames.render(context, mouseX, mouseY, delta);
            this.editFrameButton.render(context, mouseX, mouseY, delta);
            this.animationSpeedSlider.render(context, mouseX, mouseY);
            screen.scroll = !isHoveredFrameSelector();
            context.drawHorizontalLine(
                    getX() + 1,
                    getX() + getWidth() - 2,
                    getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1,
                    isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB()
            );
            context.drawCenteredTextWithShadow(
                    screen.getTextRenderer(),
                    "Frame " + frameToReplace + " Grid",
                    getWidth() - 40,
                    getY() + 23,
                    -1
            );

            context.drawTextWithShadow(screen.getTextRenderer(), "Animation Speed", getX() + 75, getY() + 28, Color.LIGHT_GRAY.getRGB());

            context.getMatrices().push();
            float scale = 0.6F;
            context.getMatrices().scale(scale, scale, 1F);

            if (viewingGrid != null) {
                WalksyLib.getInstance().get2DRenderer().renderGridOutline(
                        context,
                        viewingGrid,
                        (int) ((getWidth() - 78) / scale),
                        (int) ((getY() + 37) / scale),
                        7,
                        2,
                        MainColors.OUTLINE_WHITE.getRGB(),
                        true
                );
            }

            context.getMatrices().pop();
            this.drawScrollableFrameSelector(context, mouseX, mouseY, delta);
        }

        if (option.getValue().getCurrentFrame() != null) {
            WalksyLib.getInstance().get2DRenderer().renderGridTexture(
                    context,
                    option.getValue().getCurrentFrame(),
                    (getWidth() - 10),
                    getY() + 3,
                    1,
                    0,
                    1F
            );
        }
    }

    private boolean draggingScroller = false;
    private int dragOffsetY = 0;

    private void drawScrollableFrameSelector(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawVerticalLine(getX() + 60, getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1, getY() + getHeight() - 1, MainColors.OUTLINE_WHITE.getRGB());
        context.drawVerticalLine(getX() + 65, getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1, getY() + getHeight() - 1, MainColors.OUTLINE_WHITE.getRGB());

        int trackHeight = getHeight() - WalksyLibScreenManager.Globals.OPTION_HEIGHT;
        int contentHeight = buttonFrames.size() * 23;
        int handleHeight = MathHelper.clamp(trackHeight * trackHeight / Math.max(trackHeight, contentHeight), 10, trackHeight);

        int handleY = getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1
                + (int) (scroller.getValue() * (trackHeight - handleHeight) / Math.max(1, contentHeight - trackHeight));

        context.fill(getX() + 61, handleY + 1, getX() + 65, handleY + handleHeight, new Color(210, 210, 210).getRGB());

        context.enableScissor(getX(), getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT, getX() + 60, getY() + OPEN_HEIGHT - 1);
        for (ButtonWidget btn : buttonFrames) {
            btn.hovered = isHoveredFrameSelector();
            btn.scrollY = (float) scroller.getValue();
            btn.render(context, mouseX, mouseY, delta);
        }
        context.disableScissor();
    }

    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        super.onMouseClick(mouseX, mouseY, button);
        if (this.option.has2DPosition()) {
            this.editHudButton.onClick(mouseX, mouseY);
        }
        this.editFrameButton.onClick(mouseX, mouseY);
        this.viewFrames.onClick(mouseX, mouseY);

        int handleY = getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT + (int) scroller.getValue();
        if (isHoveringScroller()) {
            draggingScroller = true;
            dragOffsetY = (int) (mouseY - handleY);
        }

        this.animationSpeedSlider.onClick((int) mouseX, (int) mouseY, button);

        for (ButtonWidget btn : buttonFrames) {
            btn.onClick(mouseX, mouseY);
        }
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        super.onMouseRelease(mouseX, mouseY, button);
        this.animationSpeedSlider.release();
        draggingScroller = false;
    }

    @Override
    public void onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        super.onMouseDrag(mouseX, mouseY, button, deltaX, deltaY);
        if (draggingScroller) {
            int trackStart = getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1;
            int trackHeight = getHeight() - WalksyLibScreenManager.Globals.OPTION_HEIGHT;
            int contentHeight = buttonFrames.size() * 23;
            int handleHeight = MathHelper.clamp(trackHeight * trackHeight / Math.max(trackHeight, contentHeight), 10, trackHeight);

            int newValue = (int) ((mouseY - dragOffsetY - trackStart) * (contentHeight - trackHeight) / (float)(trackHeight - handleHeight));
            scroller.setValue(MathHelper.clamp(newValue, 0, Math.max(0, contentHeight - trackHeight)));
        }
        this.animationSpeedSlider.onDrag((int) mouseX);
    }

    @Override
    public void onMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        super.onMouseScroll(mouseX, mouseY, verticalAmount);
        if (isHoveredFrameSelector()) {
            scroller.onScroll(verticalAmount);
            scroller.setBounds(0, Math.max(0, buttonFrames.size() * 23 - (getHeight() - WalksyLibScreenManager.Globals.OPTION_HEIGHT)));
        }
    }

    @Override
    public void onWidgetUpdate() {
        this.editHudButton.setPosition(this.getWidth() - 82, this.getY() + 3);
        this.viewFrames.setPosition(getX() + 70, getY() + 101);
        this.editFrameButton.setPosition(getWidth() - 142, getY() + 101);
        this.updateButtons();
        scroller.setBounds(0, Math.max(0, buttonFrames.size() * 23 - (getHeight() - WalksyLibScreenManager.Globals.OPTION_HEIGHT)));
        this.animationSpeedSlider.setPos(new Point(getX() + 75, getY() + 38));
    }

    private void handleEditHudButtonClick(WalksyLibConfigScreen parent) {
        WalksyLib.getInstance().getScreenManager().openHudEditorScreen(parent, this.option);
    }

    private void handleEditFrameButtonClick() {
        if (viewingGrid == null) return;
        screen.popUp = new GridEditorPopUp(screen, viewingGrid.copy(), newGrid -> {
            this.option.setValue(PixelGridAnimation.replace(this.option.getValue(), newGrid, frameToReplace));
            this.viewingGrid = newGrid.copy();
        }, this.frameToReplace);
    }

    private void handleViewFramesButtonClick() {
        screen.popUp = new FrameManagerPopUp(screen, option, () -> this.setupFrames(this.frameToReplace), grid -> {
            this.option.getValue().setCurrentFrame(0);
        });
    }

    @Override
    public boolean isHovered() {
        return (mouseX >= getX() && mouseX < (getX() + getWidth() - 6)
                && mouseY >= getY() && mouseY < (getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT)) && !editHudButton.isHovered();
    }

    @Override
    protected void onOpen(boolean prev) {
        if (prev) {
            screen.scroll = true;
        }
    }

    public boolean isHoveredFrameSelector() {
        int xStart = getX();
        int xEnd = getX() + 65;
        int yStart = getY();
        int yEnd = getY() + getHeight() - 1;

        return mouseX >= xStart && mouseX <= xEnd &&
                mouseY >= yStart && mouseY <= yEnd;
    }

    private boolean isHoveringScroller() {
        int trackHeight = getHeight() - WalksyLibScreenManager.Globals.OPTION_HEIGHT;
        int contentHeight = buttonFrames.size() * 23;
        int handleHeight = MathHelper.clamp(trackHeight * trackHeight / Math.max(trackHeight, contentHeight), 10, trackHeight);

        int handleY = getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1
                + (int) (scroller.getValue() * (trackHeight - handleHeight) / Math.max(1, contentHeight - trackHeight));

        int scrollerX = getX() + 61;

        return mouseX >= scrollerX && mouseX <= scrollerX + 4 && mouseY >= handleY + 1 && mouseY <= handleY + handleHeight;
    }

    public void updateButtons() {
        int yOffset = 0;
        for (ButtonWidget btn : buttonFrames) {
            btn.setPosition(getX() + 5, getY() + 23 + yOffset);
            yOffset += 23;
        }
    }

    public void setupFrames(int hoverFrame) {
        buttonFrames.clear();
        this.resetViewingGrid();
        List<PixelGrid> frames = option.getValue().getFrames();

        for (int i = 0; i < frames.size(); i++) {
            int frameIndex = i;

            ButtonWidget btn = new ButtonWidget(
                    getX() + 5,
                    getY() + 23 + i * 23,
                    51,
                    18,
                    false,
                    "Frame " + (frameIndex + 1),
                    null
            );

            if (frameIndex == 0 && hoverFrame == -1) {
                btn.overrideHover = true;
                frameToReplace = 1;
            } else if (hoverFrame - 1 == frameIndex)
            {
                btn.overrideHover = true;
            }

            btn.setListener(() -> {
                buttonFrames.forEach(b -> {
                    b.overrideHover = b.isHovered();
                });
                viewingGrid = frames.get(frameIndex).copy();
                frameToReplace = frameIndex + 1;
            });

            buttonFrames.add(btn);
        }
    }

    @Override
    protected void handleResetButtonClick() {
        super.handleResetButtonClick();
        this.reset();
    }

    @Override
    public <V> void onThirdPartyChange(V value) {
        super.onThirdPartyChange(value);
        this.animationSpeedSlider.setValue(this.option.getValue().getAnimationSpeed());
    }

    public void reset() {
        this.resetViewingGrid();
        this.setupFrames(-1);
    }

    private void resetViewingGrid()
    {
        if (this.frameToReplace > 0) {
            this.viewingGrid = this.option.getValue().getFrame(1).copy();
        }
    }
}
