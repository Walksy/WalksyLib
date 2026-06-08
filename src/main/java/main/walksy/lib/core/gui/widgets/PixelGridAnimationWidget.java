package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.gui.impl.HudEditorScreen;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.impl.FrameManagerPopUp;
import main.walksy.lib.core.gui.popup.impl.GridEditorPopUp;
import main.walksy.lib.core.gui.widgets.sub.SliderSubWidget;
import main.walksy.lib.core.gui.widgets.sub.adaptor.FloatSliderAdapter;
import main.walksy.lib.core.gui.widgets.sub.adaptor.IntSliderAdapter;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.ScreenGlobals;
import main.walksy.lib.core.utils.Scroller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PixelGridAnimationWidget extends OpenableWidget {

    public final ButtonWidget editHudButton;
    public final ButtonWidget editFrameButton;
    public final ButtonWidget viewFrames;
    private final Option<PixelGridAnimation> option;
    private final SliderSubWidget<Integer> animationSpeedSlider;
    private final SliderSubWidget<Float> frameSize;
    private final List<ButtonWidget> buttonFrames = new ArrayList<>();
    private PixelGrid viewingGrid;
    private final Scroller scroller;
    private int frameToReplace = -1;

    public PixelGridAnimationWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<PixelGridAnimation> option) {
        super(parent, screen, option, x, y, width, height, option.getName(), (ScreenGlobals.OPTION_HEIGHT * 6));
        this.option = option;
        this.scroller = new Scroller(0, 2);
        this.editHudButton = new ButtonWidget(this.getWidth() - 82, this.getY() + 3, 50, 14, false, "Edit Hud", () -> this.handleEditHudButtonClick(screen));
        this.editFrameButton = new ButtonWidget(this.getWidth() - 142, this.getY() + 101, 60, 14, false, "Edit Frame", this::handleEditFrameButtonClick);
        this.viewFrames = new ButtonWidget(this.getX() + 70, this.getY() + 101, 77, 14, false, "View Frames", this::handleViewFramesButtonClick);
        this.setupFrames(-1, true);

        final List<PixelGrid> frames = option.getValue().getFrames();
        if (!frames.isEmpty()) {
            this.viewingGrid = frames.get(0).copy();
        }
        this.animationSpeedSlider = new SliderSubWidget<>(this.getX() + 75, this.getY() + 38, 100, ScreenGlobals.OPTION_HEIGHT - 12, new IntSliderAdapter(0, 20, option.getValue().getAnimationSpeed()), option.getValue().getAnimationSpeed(), option.getValue()::setAnimationSpeed, true);
        this.frameSize = new SliderSubWidget<>(this.getX() + 75, this.getY() + 68, 100, ScreenGlobals.OPTION_HEIGHT - 12, new FloatSliderAdapter(0F, 10F, option.getValue().getSize()), option.getValue().getSize(), option.getValue()::setSize, true);
    }

    @Override
    public void extract(final Graphics graphics, final int mouseX, final int mouseY, final float delta) {
        final GuiGraphicsExtractor extractor = graphics.extractor();
        super.extract(graphics, mouseX, mouseY, delta);
        this.animationSpeedSlider.setOnChange(this.option.getValue()::setAnimationSpeed);
        this.frameSize.setOnChange(this.option.getValue()::setSize);
        if (this.option.getValue().getOffsetX() != -1 && this.option.getValue().getOffsetY() != -1) {
            this.editHudButton.extractWidgetRenderState(extractor, mouseX, mouseY, delta);
        }

        extractor.verticalLine(
                this.getX() + this.getWidth() - 38,
                this.getY(),
                this.getY() + ScreenGlobals.OPTION_HEIGHT - 1,
                this.isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB()
        );

        if (!this.fullyClosed()) {
            this.viewFrames.extractWidgetRenderState(extractor, mouseX, mouseY, delta);
            this.editFrameButton.extractWidgetRenderState(extractor, mouseX, mouseY, delta);
            this.animationSpeedSlider.extract(graphics, mouseX, mouseY, delta);
            this.frameSize.extract(graphics, mouseX, mouseY, delta);
            this.screen.scroll = !this.isHoveredFrameSelector();
            extractor.horizontalLine(
                    this.getX() + 1,
                    this.getX() + this.getWidth() - 2,
                    this.getY() + ScreenGlobals.OPTION_HEIGHT - 1,
                    this.isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB()
            );
            extractor.centeredText(
                    this.screen.getFont(),
                    "Frame " + this.frameToReplace + " Grid",
                    this.getWidth() - 40,
                    this.getY() + 23,
                    -1
            );

            extractor.text(this.screen.getFont(), "Animation Speed", this.getX() + 75, this.getY() + 28, Color.LIGHT_GRAY.getRGB(), true);
            extractor.text(this.screen.getFont(), "Size", this.getX() + 75, this.getY() + 58, Color.LIGHT_GRAY.getRGB(), true);

            extractor.pose().pushMatrix();
            final float scale = 0.6F;
            extractor.pose().scale(scale, scale);

            if (this.viewingGrid != null) {
                graphics.renderGridOutline(
                        this.viewingGrid,
                        (int) ((this.getWidth() - 78) / scale),
                        (int) ((this.getY() + 37) / scale),
                        7,
                        2,
                        MainColors.OUTLINE_WHITE.getRGB(),
                        true
                );
            }

            extractor.pose().popMatrix();
            this.drawScrollableFrameSelector(extractor, mouseX, mouseY, delta);
        }

        if (this.option.getValue().getCurrentFrame() != null) {
            graphics.renderGridTexture(
                    this.option.getValue().getCurrentFrame(),
                    (this.getWidth() - 10),
                    this.getY() + 3,
                    1,
                    0,
                    false
            );
        }
    }

    private boolean draggingScroller = false;
    private int dragOffsetY = 0;

    private void drawScrollableFrameSelector(final GuiGraphicsExtractor extractor, final int mouseX, final int mouseY, final float delta) {
        extractor.verticalLine(this.getX() + 60, this.getY() + ScreenGlobals.OPTION_HEIGHT - 1, this.getY() + this.getHeight() - 1, MainColors.OUTLINE_WHITE.getRGB());
        extractor.verticalLine(this.getX() + 65, this.getY() + ScreenGlobals.OPTION_HEIGHT - 1, this.getY() + this.getHeight() - 1, MainColors.OUTLINE_WHITE.getRGB());

        final int trackHeight = this.getHeight() - ScreenGlobals.OPTION_HEIGHT;
        final int contentHeight = this.buttonFrames.size() * 23;
        final int handleHeight = Mth.clamp(trackHeight * trackHeight / Math.max(trackHeight, contentHeight), 10, trackHeight);

        final int handleY = this.getY() + ScreenGlobals.OPTION_HEIGHT - 1
                + (int) (this.scroller.getValue() * (trackHeight - handleHeight) / Math.max(1, contentHeight - trackHeight));

        extractor.fill(this.getX() + 61, handleY + 1, this.getX() + 65, handleY + handleHeight, new Color(210, 210, 210).getRGB());

        extractor.enableScissor(this.getX(), this.getY() + ScreenGlobals.OPTION_HEIGHT, this.getX() + 60, this.getY() + this.OPEN_HEIGHT - 1);
        for (final ButtonWidget btn : this.buttonFrames) {
            btn.hovered = this.isHoveredFrameSelector();
            btn.scrollY = (float) this.scroller.getValue();
            btn.extractWidgetRenderState(extractor, mouseX, mouseY, delta);
        }
        extractor.disableScissor();
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        super.onMouseClick(click, doubled);
        if (this.option.getValue().getOffsetX() != -1 && this.option.getValue().getOffsetY() != -1) {
            this.editHudButton.onClick(click, doubled);
        }
        this.editFrameButton.onClick(click, doubled);
        this.viewFrames.onClick(click, doubled);

        final int handleY = this.getY() + ScreenGlobals.OPTION_HEIGHT + (int) this.scroller.getValue();
        if (this.isHoveringScroller()) {
            this.draggingScroller = true;
            this.dragOffsetY = (int) (this.mouseY - handleY);
        }

        this.animationSpeedSlider.onClick(click, doubled);
        this.frameSize.onClick(click, doubled);

        for (final ButtonWidget btn : this.buttonFrames) {
            btn.onClick(click, doubled);
        }
    }

    @Override
    public void onMouseRelease(final MouseButtonEvent click) {
        super.onMouseRelease(click);
        this.animationSpeedSlider.release();
        this.frameSize.release();
        this.draggingScroller = false;
    }

    @Override
    public void onMouseDrag(final MouseButtonEvent click, final double deltaX, final double deltaY) {
        super.onMouseDrag(click, deltaX, deltaY);
        if (this.draggingScroller) {
            final int trackStart = this.getY() + ScreenGlobals.OPTION_HEIGHT - 1;
            final int trackHeight = this.getHeight() - ScreenGlobals.OPTION_HEIGHT;
            final int contentHeight = this.buttonFrames.size() * 23;
            final int handleHeight = Mth.clamp(trackHeight * trackHeight / Math.max(trackHeight, contentHeight), 10, trackHeight);

            final int newValue = (int) ((this.mouseY - this.dragOffsetY - trackStart) * (contentHeight - trackHeight) / (float) (trackHeight - handleHeight));
            this.scroller.setValue(Mth.clamp(newValue, 0, Math.max(0, contentHeight - trackHeight)));
        }
        this.animationSpeedSlider.onDrag((int) this.mouseX);
        this.frameSize.onDrag((int) this.mouseX);
    }

    @Override
    public void onMouseScroll(final double mouseX, final double mouseY, final double verticalAmount) {
        super.onMouseScroll(mouseX, mouseY, verticalAmount);
        if (this.isHoveredFrameSelector()) {
            this.scroller.onScroll(verticalAmount);
            this.scroller.setBounds(0, Math.max(0, this.buttonFrames.size() * 23 - (this.getHeight() - ScreenGlobals.OPTION_HEIGHT)));
        }
    }

    @Override
    public void onWidgetUpdate() {
        this.editHudButton.setPosition(this.getWidth() - 82, this.getY() + 3);
        this.viewFrames.setPosition(this.getX() + 70, this.getY() + 101);
        this.editFrameButton.setPosition(this.getWidth() - 142, this.getY() + 101);
        this.updateButtons();
        this.scroller.setBounds(0, Math.max(0, this.buttonFrames.size() * 23 - (this.getHeight() - ScreenGlobals.OPTION_HEIGHT)));
        this.animationSpeedSlider.setPos(new Point(this.getX() + 75, this.getY() + 38));
        this.frameSize.setPos(new Point(this.getX() + 75, this.getY() + 68));
    }

    private void handleEditHudButtonClick(final WalksyLibConfigScreen parent) {
        Minecraft.getInstance().setScreenAndShow(new HudEditorScreen(parent, this.option));
    }

    private void handleEditFrameButtonClick() {
        if (this.viewingGrid == null) return;
        this.screen.popUp = new GridEditorPopUp(this.screen, this.viewingGrid.copy(), newGrid -> {
            this.option.setValue(PixelGridAnimation.replace(this.option.getValue(), newGrid, this.frameToReplace));
            this.viewingGrid = newGrid.copy();
            this.setupFrames(this.frameToReplace, false);
        }, this.frameToReplace);
    }

    private void handleViewFramesButtonClick() {
        this.screen.popUp = new FrameManagerPopUp(
                this.screen,
                this.option,
                () -> {
                    this.setupFrames(this.frameToReplace, false);

                    final PixelGrid frame = this.option.getValue().getFrame(this.frameToReplace);
                    final PixelGrid grid;

                    if (frame != null) {
                        grid = frame.copy();
                    } else {
                        grid = this.option.getValue().getFrames().get(0).copy();
                    }

                    this.viewingGrid = grid;
                },
                grid -> this.option.getValue().setCurrentFrame(0)
        );
    }


    @Override
    public boolean isHovered() {
        return (this.mouseX >= this.getX() && this.mouseX < (this.getX() + this.getWidth() - 6)
                && this.mouseY >= this.getY() && this.mouseY < (this.getY() + ScreenGlobals.OPTION_HEIGHT)) && !this.editHudButton.isHovered();
    }

    @Override
    protected void onOpen(final boolean prev) {
        if (prev) {
            this.screen.scroll = true;
        }
    }

    public boolean isHoveredFrameSelector() {
        final int xStart = this.getX();
        final int xEnd = this.getX() + 65;
        final int yStart = this.getY();
        final int yEnd = this.getY() + this.getHeight() - 1;

        return this.mouseX >= xStart && this.mouseX <= xEnd &&
                this.mouseY >= yStart && this.mouseY <= yEnd;
    }

    private boolean isHoveringScroller() {
        final int trackHeight = this.getHeight() - ScreenGlobals.OPTION_HEIGHT;
        final int contentHeight = this.buttonFrames.size() * 23;
        final int handleHeight = Mth.clamp(trackHeight * trackHeight / Math.max(trackHeight, contentHeight), 10, trackHeight);

        final int handleY = this.getY() + ScreenGlobals.OPTION_HEIGHT - 1
                + (int) (this.scroller.getValue() * (trackHeight - handleHeight) / Math.max(1, contentHeight - trackHeight));

        final int scrollerX = this.getX() + 61;

        return this.mouseX >= scrollerX && this.mouseX <= scrollerX + 4 && this.mouseY >= handleY + 1 && this.mouseY <= handleY + handleHeight;
    }

    public void updateButtons() {
        int yOffset = 0;
        for (final ButtonWidget btn : this.buttonFrames) {
            btn.setPosition(this.getX() + 5, this.getY() + 23 + yOffset);
            yOffset += 23;
        }
    }

    public void setupFrames(final int hoverFrame, final boolean reset) {
        this.buttonFrames.clear();
        if (reset) {
            this.resetViewingGrid();
        }
        final List<PixelGrid> frames = this.option.getValue().getFrames();

        for (int i = 0; i < frames.size(); i++) {
            final int frameIndex = i;

            final ButtonWidget btn = new ButtonWidget(
                    this.getX() + 5,
                    this.getY() + 23 + i * 23,
                    51,
                    18,
                    false,
                    "Frame " + (frameIndex + 1),
                    null
            );

            if (frameIndex == 0 && hoverFrame == -1) {
                btn.overrideHover = true;
                this.frameToReplace = 1;
            } else if (hoverFrame - 1 == frameIndex) {
                btn.overrideHover = true;
            }

            btn.setListener(() -> {
                this.buttonFrames.forEach(b -> {
                    b.overrideHover = b.isHovered();
                });
                this.viewingGrid = frames.get(frameIndex).copy();
                this.frameToReplace = frameIndex + 1;
            });

            this.buttonFrames.add(btn);
        }
    }

    @Override
    protected void handleResetButtonClick() {
        super.handleResetButtonClick();
        this.reset();
    }

    @Override
    public <V> void onThirdPartyChange(final V value) {
        super.onThirdPartyChange(value);
        this.animationSpeedSlider.setValue(this.option.getValue().getAnimationSpeed());
        this.frameSize.setValue(this.option.getValue().getSize());
    }

    public void reset() {
        this.resetViewingGrid();
        this.setupFrames(-1, true);
    }

    private void resetViewingGrid() {
        if (this.frameToReplace > 0) {
            this.viewingGrid = this.option.getValue().getFrame(1).copy();
        }
    }
}
