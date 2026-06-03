package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.sub.SliderSubWidget;
import main.walksy.lib.core.gui.widgets.sub.TextboxSubWidget;
import main.walksy.lib.core.gui.widgets.sub.adaptor.IntSliderAdapter;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.ScreenGlobals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.awt.*;

public class ColorWidget extends OpenableWidget {
    private final Option<WalksyLibColor> option;

    public int COLOR_PICKER_STARTX;
    private final Identifier TRANSPARENT_BACKGROUND = Identifier.fromNamespaceAndPath("walksylib", "gui/widget/transparent.png");
    private final Identifier RAINBOW_ICON = Identifier.fromNamespaceAndPath("walksylib", "gui/widget/rainbow.png");
    private final Identifier PULSE_ICON = Identifier.fromNamespaceAndPath("walksylib", "gui/widget/pulse.png");
    private final ButtonWidget chromaButton;
    private final ButtonWidget pulseButton;
    private final SliderSubWidget<Integer> chromaSpeedSlider;
    private final SliderSubWidget<Integer> pulseSpeedSlider;
    private final Animation satThumbXAnim = new Animation(0, 0.5f);
    private final Animation satThumbYAnim = new Animation(0, 0.5f);
    private final Animation hueThumbYAnim = new Animation(0, 0.5f);
    private final Animation opacityThumbYAnim = new Animation(0, 0.5f);

    private final TextboxSubWidget hexInput;
    private boolean updatingHex = false;

    private enum DragTarget {
        NONE,
        HUE_SLIDER,
        SATURATION_VALUE_BOX,
        OPACITY_SLIDER
    }

    private DragTarget activeDrag = DragTarget.NONE;

    public ColorWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<WalksyLibColor> option) {
        super(parent, screen, option, x, y, width, height, option.getName(), ScreenGlobals.OPTION_HEIGHT * 5);
        this.option = option;
        final WalksyLibColor initial = option.getValue();
        final float[] hsb = WalksyLibColor.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
        option.getValue().setHue(hsb[0]);
        option.getValue().setSaturation(hsb[1]);
        option.getValue().setBrightness(hsb[2]);
        option.getValue().setAlpha(initial.getAlpha());
        this.COLOR_PICKER_STARTX = (this.getX() + this.getWidth()) / 2;
        this.chromaButton = new ButtonWidget(x + 5, y + 19 + 10, 17, 17, false, RAINBOW_ICON, () -> this.option.getValue().setRainbow(!this.option.getValue().isRainbow()), -3, -3);
        this.pulseButton = new ButtonWidget(x + 5, y + 45 + 10, 17, 17, false, PULSE_ICON, () -> this.option.getValue().setPulse(!this.option.getValue().isPulse()), -3, -3);
        this.chromaSpeedSlider = new SliderSubWidget<>(x + 28, y + 23 + 10, this.COLOR_PICKER_STARTX - 32 - 60, ScreenGlobals.OPTION_HEIGHT - 12, new IntSliderAdapter(1, 20, this.option.getValue().getRainbowSpeed()), this.option.getValue().getRainbowSpeed(), rainbowSpeed -> {
            this.option.getValue().setRainbowSpeed(rainbowSpeed);
            this.updateThumbTargets(false);
        }, true);
        this.pulseSpeedSlider = new SliderSubWidget<>(x + 28, y + 49 + 10, this.COLOR_PICKER_STARTX - 32 - 60, ScreenGlobals.OPTION_HEIGHT - 12, new IntSliderAdapter(1, 20, this.option.getValue().getPulseSpeed()), this.option.getValue().getPulseSpeed(), pulseSpeed -> {
            this.option.getValue().setPulseSpeed(pulseSpeed);
            this.updateThumbTargets(false);
        }, true);
        final String initialHex = String.format("#%02X%02X%02X%02X",
                initial.getRed(), initial.getGreen(), initial.getBlue(), initial.getAlpha());
        this.hexInput = new TextboxSubWidget(screen, 0, 0, 78, 78, 14, initialHex, str -> {}, false);
        this.hexInput.setOnFocusLost(() -> {
            final String hexStr = this.hexInput.getText();
            final String clean = hexStr.startsWith("#") ? hexStr.substring(1) : hexStr;
            if (clean.length() != 6 && clean.length() != 8) {
                this.syncHexInput();
                return;
            }
            try {
                final int r = Integer.parseInt(clean.substring(0, 2), 16);
                final int g = Integer.parseInt(clean.substring(2, 4), 16);
                final int b = Integer.parseInt(clean.substring(4, 6), 16);
                final int a = clean.length() == 8
                        ? Integer.parseInt(clean.substring(6, 8), 16)
                        : option.getValue().getAlpha();
                final float[] parsedHsb = WalksyLibColor.RGBtoHSB(r, g, b, null);
                option.getValue().setHue(parsedHsb[0]);
                option.getValue().setSaturation(parsedHsb[1]);
                option.getValue().setBrightness(parsedHsb[2]);
                option.getValue().setAlpha(a);
                final WalksyLibColor updated = new WalksyLibColor(r, g, b, a);
                updated.setAdditions(option.getValue().getAdditions());
                option.setValue(updated);
                this.updateThumbTargets(false);
                this.syncHexInput();
            } catch (NumberFormatException ignored) {
                this.syncHexInput();
            }
        });
    }

    @Override
    public void extract(final Graphics graphics, final int mouseX, final int mouseY, final float delta) {
        final GuiGraphicsExtractor extractor = graphics.extractor();
        super.extract(graphics, mouseX, mouseY, delta);
        this.pulseSpeedSlider.setOnChange(this.option.getValue()::setPulseSpeed);
        this.chromaSpeedSlider.setOnChange(this.option.getValue()::setRainbowSpeed);
        this.COLOR_PICKER_STARTX = (this.getX() + this.getWidth()) / 2;
        final int baseHeight = ScreenGlobals.OPTION_HEIGHT;
        this.satThumbXAnim.update(delta);
        this.satThumbYAnim.update(delta);
        this.hueThumbYAnim.update(delta);
        this.opacityThumbYAnim.update(delta);
        graphics.drawRoundedTexture(RenderPipelines.GUI_TEXTURED, TRANSPARENT_BACKGROUND, this.getWidth() - 15, this.getY() + 4, 23, baseHeight - 8, 2, 4, 4);
        graphics.fillRoundedRectOutline(this.getWidth() - 16, this.getY() + 3, 25, baseHeight - 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        graphics.fillRoundedRect(this.getWidth() - 15, this.getY() + 4, 23, baseHeight - 8, 2, this.option.getValue().getRGB());

        this.hexInput.extract(extractor, mouseX, mouseY, delta);

        if (this.fullyClosed()) {
            extractor.verticalLine(this.getX() + this.getWidth() - 38, this.getY(), this.getY() + ScreenGlobals.OPTION_HEIGHT - 1, this.isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        } else if (this.getCurrentHeight() > 34) {
            this.drawHueSlider(graphics);
            this.drawSaturationBox(graphics);
            this.drawOpacitySlider(graphics);
            this.chromaButton.overrideHover = this.option.getValue().isRainbow();
            this.pulseButton.overrideHover = this.option.getValue().isPulse();
            this.chromaButton.extractWidgetRenderState(extractor, mouseX, mouseY, delta);
            this.pulseButton.extractWidgetRenderState(extractor, mouseX, mouseY, delta);
            this.chromaSpeedSlider.extract(extractor, mouseX, mouseY, delta);
            this.pulseSpeedSlider.extract(extractor, mouseX, mouseY, delta);

            extractor.text(
                    Minecraft.getInstance().font,
                    "Rainbow Speed",
                    this.getX() + 32,
                    this.getY() + 23,
                    Color.LIGHT_GRAY.getRGB(),
                    true
            );

            extractor.text(
                    Minecraft.getInstance().font,
                    "Pulse Speed",
                    this.getX() + 32,
                    this.getY() + 49,
                    Color.LIGHT_GRAY.getRGB(),
                    true
            );
        }
    }

    public void drawSatThumb(final Graphics graphics, final int x, final int y) {
        graphics.fillRoundedRectOutline(x, y, 6, 6, 1, 1, Color.BLACK.getRGB());
        graphics.fillRoundedRectOutline(x + 1, y + 1, 4, 4, 1, 1, MainColors.OUTLINE_WHITE.getRGB());
    }

    public void drawSliderThumb(final Graphics graphics, final int x, final int y) {
        graphics.fillRoundedRectOutline(x, y, 9, 3, 1, 1, Color.BLACK.getRGB());
    }

    public void drawOpacitySlider(final Graphics graphics) {
        final int opacityHeight = this.getHeight() - 30 + 8;
        graphics.fillRoundedRectOutline(this.COLOR_PICKER_STARTX - 34, this.getY() + 19, 15, opacityHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        graphics.fillRoundedRectOutline(this.COLOR_PICKER_STARTX - 33, this.getY() + 20, 13, opacityHeight - 2, 2, 1, this.isHoveringOpacitySlider(this.mouseX, this.mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        graphics.drawRoundedTexture(RenderPipelines.GUI_TEXTURED, TRANSPARENT_BACKGROUND, this.COLOR_PICKER_STARTX - 32, this.getY() + 21, 11, opacityHeight - 4, 2, 4, 4);
        final WalksyLibColor color = new WalksyLibColor(this.option.getValue().getRed(), this.option.getValue().getGreen(), this.option.getValue().getBlue(), 255);
        final WalksyLibColor colorG = new WalksyLibColor(this.option.getValue().getRed(), this.option.getValue().getGreen(), this.option.getValue().getBlue(), 0);
        graphics.fillRoundedRectGradient(this.COLOR_PICKER_STARTX - 32, this.getY() + 21, 11, opacityHeight - 4, 2, color.getRGB(), colorG.getRGB());

        final int opacitySliderX = this.COLOR_PICKER_STARTX - 34;
        this.drawSliderThumb(graphics, opacitySliderX + 3, (int) this.opacityThumbYAnim.getCurrentValue() + 1);
    }

    public void drawHueSlider(final Graphics graphics) {
        final int hueHeight = this.getHeight() - 30 + 8;
        graphics.fillRoundedRectOutline(this.COLOR_PICKER_STARTX - 17, this.getY() + 19, 15, hueHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        graphics.fillRoundedRectOutline(this.COLOR_PICKER_STARTX - 16, this.getY() + 20, 13, hueHeight - 2, 2, 1, this.isHoveringHueSlider(this.mouseX, this.mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        graphics.drawRoundedHueSlider(this.COLOR_PICKER_STARTX - 15, this.getY() + 21, 11, hueHeight - 4, 2);

        final int hueSliderX = this.COLOR_PICKER_STARTX - 17;

        this.drawSliderThumb(graphics, hueSliderX + 3, (int) this.hueThumbYAnim.getCurrentValue() + 1);
    }

    public void drawSaturationBox(final Graphics graphics) {
        graphics.fillRoundedRectOutline(this.COLOR_PICKER_STARTX, this.getY() + 19, this.getWidth() - this.COLOR_PICKER_STARTX + 9, this.getHeight() - 28 + 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        graphics.fillRoundedRectOutline(this.COLOR_PICKER_STARTX + 1, this.getY() + 20, this.getWidth() - this.COLOR_PICKER_STARTX + 7, this.getHeight() - 28 + 6 - 2, 2, 1, this.isHoveringSaturationValueBox(this.mouseX, this.mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        graphics.drawHueSaturationValueBox(this.COLOR_PICKER_STARTX + 2, this.getY() + 21, this.getWidth() - this.COLOR_PICKER_STARTX + 5, this.getHeight() - 28 + 6 - 4, 2, this.option.getValue().getHue());
        this.drawSatThumb(graphics, (int) this.satThumbXAnim.getCurrentValue() + 1, (int) this.satThumbYAnim.getCurrentValue());
    }

    public boolean isHoveringHueSlider(final double mouseX, final double mouseY) {
        final int x = this.COLOR_PICKER_STARTX - 17;
        final int y = this.getY() + 19;
        final int width = 15;
        final int height = this.getHeight() - 30 + 8;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isHoveringSaturationValueBox(final double mouseX, final double mouseY) {
        final int x = this.COLOR_PICKER_STARTX;
        final int y = this.getY() + 19;
        final int width = this.getWidth() - this.COLOR_PICKER_STARTX + 9;
        final int height = this.getHeight() - 28 + 6;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isHoveringOpacitySlider(final double mouseX, final double mouseY) {
        final int x = this.COLOR_PICKER_STARTX - 34;
        final int y = this.getY() + 19;
        final int width = 15;
        final int height = this.getHeight() - 30 + 8;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isHoveringRainbowButton(final double mouseX, final double mouseY) {
        final int x = this.COLOR_PICKER_STARTX - 54;
        final int y = this.getY() + 19;
        final int width = 18;
        final int height = (this.getHeight() - 85) + 3;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        super.onMouseClick(click, doubled);

        final double mouseX = click.x();
        final double mouseY = click.y();
        final int button = click.button();

        this.chromaSpeedSlider.onClick(click, doubled);
        this.pulseSpeedSlider.onClick(click, doubled);
        this.chromaButton.onClick(click, doubled);
        this.pulseButton.onClick(click, doubled);
        this.hexInput.onClick(click, doubled);
        if (button != 0) return;

        if (this.open) {
            if (this.isHoveringHueSlider(mouseX, mouseY)) {
                this.activeDrag = DragTarget.HUE_SLIDER;
                this.handleHueSliderClick(mouseY);
                this.onChange();
                return;
            }
            if (this.isHoveringSaturationValueBox(mouseX, mouseY)) {
                this.activeDrag = DragTarget.SATURATION_VALUE_BOX;
                this.handleSatValBoxClick(mouseX, mouseY);
                this.onChange();
                return;
            }
            if (this.isHoveringOpacitySlider(mouseX, mouseY)) {
                this.activeDrag = DragTarget.OPACITY_SLIDER;
                this.handleOpacitySliderClick(mouseY);
                this.onChange();
                return;
            }
        }
    }

    @Override
    public void onKeyPress(final KeyEvent input) {
        this.hexInput.onKeyPress(input);
    }

    @Override
    public void onCharTyped(final CharacterEvent input) {
        this.hexInput.onCharTyped(input);
    }

    @Override
    protected void onOpen(final boolean prev) {}

    @Override
    public boolean isHovered() {
        return this.mouseX >= this.getX() && this.mouseX < (this.getX() + this.getWidth() - 6)
                && this.mouseY >= this.getY() && this.mouseY < (this.getY() + ScreenGlobals.OPTION_HEIGHT) && this.isVisible() && this.isAvailable() && !this.hexInput.hovered;
    }

    @Override
    public void onMouseRelease(final MouseButtonEvent click) {
        if (click.button() != 0) return;
        this.activeDrag = DragTarget.NONE;
    }

    @Override
    public void onMouseDrag(final MouseButtonEvent click, final double offsetX, final double offsetY) {
        this.chromaSpeedSlider.onDrag((int) this.mouseX);
        this.pulseSpeedSlider.onDrag((int) this.mouseX);
        if (click.button() != 0) return;

        switch (this.activeDrag) {
            case HUE_SLIDER -> this.handleHueSliderDrag(this.mouseY);
            case SATURATION_VALUE_BOX -> this.handleSatValBoxDrag(this.mouseX, this.mouseY);
            case OPACITY_SLIDER -> this.handleOpacitySliderDrag(this.mouseY);
            default -> {
            }
        }
        this.onChange();
    }

    @Override
    public void onChange() {
        super.onChange();
        this.updateThumbTargets(false);
    }

    @Override
    public void onRelease(final MouseButtonEvent click) {
        this.chromaSpeedSlider.release();
        this.pulseSpeedSlider.release();
    }

    @Override
    public void onWidgetUpdate() {
        this.chromaSpeedSlider.setPos(new Point(this.getX() + 28, this.getY() + 23 + 10));
        this.pulseSpeedSlider.setPos(new Point(this.getX() + 28, this.getY() + 49 + 10));
        this.chromaButton.setX(this.getX() + 5);
        this.chromaButton.setY(this.getY() + 29);
        this.pulseButton.setX(this.getX() + 5);
        this.pulseButton.setY(this.getY() + 55);
        this.hexInput.setPos(new Point(this.getX() + this.getWidth() - 98, this.getTextYCentered() - 3));
        final int width = ((this.getX() + this.getWidth()) / 2) - 32 - 60;
        this.chromaSpeedSlider.setWidth(width);
        this.pulseSpeedSlider.setWidth(width);
        this.updateThumbTargets(true);
    }

    @Override
    public <V> void onThirdPartyChange(final V value) {
        super.onThirdPartyChange(value);
        this.pulseSpeedSlider.setValue(this.option.getValue().getPulseSpeed());
        this.chromaSpeedSlider.setValue(this.option.getValue().getRainbowSpeed());
        this.syncHexInput();
    }

    private void updateThumbTargets(final boolean jump) {
        final int hueSliderY = this.getY() + 21;
        final int hueSliderHeight = this.getHeight() - 30 + 8 - 4;
        final int rawHueThumbY = hueSliderY + (int) ((1f - this.option.getValue().getHue()) * hueSliderHeight) - 2;
        final int targetHueY = Math.max(hueSliderY - 1, Math.min(rawHueThumbY - 1, hueSliderY + hueSliderHeight - 2));
        final int opacitySliderY = this.getY() + 21;
        final int opacitySliderHeight = this.getHeight() - 30 + 8 - 4;
        final int rawOpacityThumbY = opacitySliderY + (int) ((1f - (this.option.getValue().getAlpha() / 255f)) * opacitySliderHeight) - 2;
        final int targetOpacityY = Math.max(opacitySliderY - 1, Math.min(rawOpacityThumbY - 1, opacitySliderY + opacitySliderHeight - 2));
        final int boxX = this.COLOR_PICKER_STARTX;
        final int boxY = this.getY() + 21;
        final int boxWidth = this.getWidth() - this.COLOR_PICKER_STARTX + 9;
        final int boxHeight = this.getHeight() - 28 + 6 - 4;
        final int rawThumbX = boxX + (int) (this.option.getValue().getSaturation() * boxWidth) + 3;
        final int rawThumbY = boxY + (int) ((1f - this.option.getValue().getBrightness()) * boxHeight) + 1;

        if (jump) {
            this.hueThumbYAnim.jumpTo(targetHueY);
            this.satThumbXAnim.jumpTo(Math.max(boxX + 1, Math.min(rawThumbX - 6, boxX + boxWidth - 8)));
            this.satThumbYAnim.jumpTo(Math.max(boxY, Math.min(rawThumbY - 4, boxY + boxHeight - 6)));
            this.opacityThumbYAnim.jumpTo(targetOpacityY);
        } else {
            this.hueThumbYAnim.setTargetValue(targetHueY);
            this.satThumbXAnim.setTargetValue(Math.max(boxX + 1, Math.min(rawThumbX - 6, boxX + boxWidth - 8)));
            this.satThumbYAnim.setTargetValue(Math.max(boxY, Math.min(rawThumbY - 4, boxY + boxHeight - 6)));
            this.opacityThumbYAnim.setTargetValue(targetOpacityY);
        }
    }

    private void handleHueSliderClick(final double mouseY) {
        final float newHue = 1f - (float) ((mouseY - (this.getY() + 20)) / (this.getHeight() - 30 + 6));
        this.option.getValue().setHue(Mth.clamp(newHue, 0f, 1f));
        this.updateColor();
    }

    private void handleHueSliderDrag(final double mouseY) {
        this.handleHueSliderClick(mouseY);
    }

    private void handleSatValBoxClick(final double mouseX, final double mouseY) {
        final int boxX = this.COLOR_PICKER_STARTX;
        final int boxY = this.getY() + 20;
        final int boxWidth = this.getWidth() - this.COLOR_PICKER_STARTX + 9;
        final int boxHeight = this.getHeight() - 29 + 6;
        final float newSaturation = (float) ((mouseX - boxX) / (double) boxWidth);
        final float newBrightness = 1f - (float) ((mouseY - boxY) / (double) boxHeight);

        if (mouseX < boxX) {
            this.option.getValue().setSaturation(0f);
        } else if (mouseX > boxX + boxWidth) {
            this.option.getValue().setSaturation(1f);
        } else {
            this.option.getValue().setSaturation(Mth.clamp(newSaturation, 0f, 1f));
        }
        if (mouseY < boxY) {
            this.option.getValue().setBrightness(1f);
        } else if (mouseY > boxY + boxHeight) {
            this.option.getValue().setBrightness(0f);
        } else {
            this.option.getValue().setBrightness(Mth.clamp(newBrightness, 0f, 1f));
        }

        this.updateColor();
    }

    private void handleSatValBoxDrag(final double mouseX, final double mouseY) {
        this.handleSatValBoxClick(mouseX, mouseY);
    }

    private void handleOpacitySliderClick(final double mouseY) {
        final int sliderY = this.getY() + 20;
        final int sliderHeight = this.getHeight() - 30 + 6;

        final float newOpacity = 1f - (float) ((mouseY - sliderY) / (double) sliderHeight);
        this.option.getValue().setAlpha((int) (Mth.clamp(newOpacity, 0f, 1f) * 255));

        this.updateColor();
    }

    private void handleOpacitySliderDrag(final double mouseY) {
        this.handleOpacitySliderClick(mouseY);
    }

    private void updateColor() {
        final int rgb = WalksyLibColor.HSBtoRGB(this.option.getValue().getHue(), this.option.getValue().getSaturation(), this.option.getValue().getBrightness());
        final WalksyLibColor updated = new WalksyLibColor(
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF,
                this.option.getValue().getAlpha()
        );
        updated.setAdditions(this.option.getValue().getAdditions());
        this.option.setValue(updated);
        this.syncHexInput();
    }

    private void syncHexInput() {
        this.updatingHex = true;
        this.hexInput.setText(String.format("#%02X%02X%02X%02X",
                this.option.getValue().getRed(),
                this.option.getValue().getGreen(),
                this.option.getValue().getBlue(),
                this.option.getValue().getAlpha()
        ));
        this.updatingHex = false;
    }
}
