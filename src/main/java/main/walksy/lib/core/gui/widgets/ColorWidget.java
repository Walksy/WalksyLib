package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import main.walksy.lib.core.gui.widgets.sub.SliderSubWidget;
import main.walksy.lib.core.gui.widgets.sub.adaptor.IntSliderAdapter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.Renderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ColorWidget extends OpenableWidget {
    private final Option<WalksyLibColor> option;

    public int COLOR_PICKER_STARTX;
    private final Identifier TRANSPARENT_BACKGROUND = Identifier.of("walksylib", "gui/widget/transparent.png");
    private final Identifier RAINBOW_ICON = Identifier.of("walksylib", "gui/widget/rainbow.png");
    private final Identifier PULSE_ICON = Identifier.of("walksylib", "gui/widget/pulse.png");
    private final ButtonWidget chromaButton;
    private final ButtonWidget pulseButton;
    private final SliderSubWidget<Integer> chromaSpeedSlider;
    private final SliderSubWidget<Integer> pulseSpeedSlider;

    private enum DragTarget {
        NONE,
        HUE_SLIDER,
        SATURATION_VALUE_BOX,
        OPACITY_SLIDER
    }

    private DragTarget activeDrag = DragTarget.NONE;

    public ColorWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<WalksyLibColor> option) {
        super(parent, screen, option, x, y, width, height, option.getName(), WalksyLibScreenManager.Globals.OPTION_HEIGHT * 5);
        this.option = option;
        WalksyLibColor initial = option.getValue();
        float[] hsb = WalksyLibColor.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
        option.getValue().setHue(hsb[0]);
        option.getValue().setSaturation(hsb[1]);
        option.getValue().setBrightness(hsb[2]);
        option.getValue().setAlpha(initial.getAlpha());
        COLOR_PICKER_STARTX = (getX() + getWidth()) / 2;
        this.chromaButton = new ButtonWidget(x + 5, y + 19 + 10, 17, 17, false, RAINBOW_ICON, () -> this.option.getValue().setRainbow(!this.option.getValue().isRainbow()), -3, -3);
        this.pulseButton = new ButtonWidget(x + 5, y + 45 + 10, 17, 17, false, PULSE_ICON, () -> this.option.getValue().setPulse(!this.option.getValue().isPulse()), -3, -3);
        this.chromaSpeedSlider = new SliderSubWidget<>(x + 28, y + 23 + 10, COLOR_PICKER_STARTX - 32 - 60, WalksyLibScreenManager.Globals.OPTION_HEIGHT - 12, new IntSliderAdapter(1, 20, this.option.getValue().getRainbowSpeed()), this.option.getValue().getRainbowSpeed(), this.option.getValue()::setRainbowSpeed, true);
        this.pulseSpeedSlider = new SliderSubWidget<>(x + 28, y + 49 + 10, COLOR_PICKER_STARTX - 32 - 60, WalksyLibScreenManager.Globals.OPTION_HEIGHT - 12, new IntSliderAdapter(1, 20, this.option.getValue().getPulseSpeed()), this.option.getValue().getPulseSpeed(), this.option.getValue()::setPulseSpeed, true);
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
        //if the option value object changes (via a reset & object copying etc...), the slider's consumer will point to the wrong object
        this.pulseSpeedSlider.setOnChange(this.option.getValue()::setPulseSpeed);
        this.chromaSpeedSlider.setOnChange(this.option.getValue()::setRainbowSpeed);
        COLOR_PICKER_STARTX = (getX() + getWidth()) / 2;
        int baseHeight = WalksyLibScreenManager.Globals.OPTION_HEIGHT;

        Renderer.drawRoundedTexture(context, RenderLayer::getGuiTextured, TRANSPARENT_BACKGROUND, getWidth() - 15, getY() + 4, 23, baseHeight - 8, 2, 4, 4);
        Renderer.fillRoundedRectOutline(context, getWidth() - 16, getY() + 3, 25, baseHeight - 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer.fillRoundedRect(context, getWidth() - 15, getY() + 4, 23, baseHeight - 8, 2, option.getValue().getRGB());

        context.drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                String.format("#%02X%02X%02X%02X",
                        option.getValue().getRed(),
                        option.getValue().getGreen(),
                        option.getValue().getBlue(),
                        option.getValue().getAlpha()
                ),
                getX() + getWidth() - 98,
                getTextYCentered() + 1,
                java.awt.Color.LIGHT_GRAY.getRGB()
        );

        if (!open) {
            context.drawVerticalLine(getX() + getWidth() - 38, getY(), getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        } else {
            drawHueSlider(context);
            drawSaturationBox(context);
            drawOpacitySlider(context);
            this.chromaButton.overrideHover = this.option.getValue().isRainbow();
            this.pulseButton.overrideHover = this.option.getValue().isPulse();
            this.chromaButton.render(context, mouseX, mouseY, delta);
            this.pulseButton.render(context, mouseX, mouseY, delta);
            this.chromaSpeedSlider.render(context, mouseX, mouseY);
            this.pulseSpeedSlider.render(context, mouseX, mouseY);

            context.drawTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    "Rainbow Speed",
                    getX() + 32,
                    getY() + 23,
                    java.awt.Color.LIGHT_GRAY.getRGB()
            );

            context.drawTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    "Pulse Speed",
                    getX() + 32,
                    getY() + 49,
                    java.awt.Color.LIGHT_GRAY.getRGB()
            );
        }
    }

    public void drawSatThumb(DrawContext context, int x, int y)
    {
        Renderer.fillRoundedRectOutline(context, x, y, 6, 6, 1, 1, java.awt.Color.BLACK.getRGB());
        Renderer.fillRoundedRectOutline(context, x + 1, y + 1, 4, 4, 1, 1, MainColors.OUTLINE_WHITE.getRGB());
    }

    public void drawSliderThumb(DrawContext context, int x, int y)
    {
        Renderer.fillRoundedRectOutline(context, x, y, 9, 3, 1, 1, java.awt.Color.BLACK.getRGB());
    }

    public void drawOpacitySlider(DrawContext  context)
    {
        int opacityHeight = getHeight() - 30 + 8;
        Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 34, getY() + 19, 15, opacityHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 33, getY() + 20, 13, opacityHeight - 2, 2, 1, isHoveringOpacitySlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer.drawRoundedTexture(context, RenderLayer::getGuiTextured, TRANSPARENT_BACKGROUND, COLOR_PICKER_STARTX - 32, getY() + 21, 11, opacityHeight - 4, 2, 4, 4);
        WalksyLibColor color = new WalksyLibColor(option.getValue().getRed(), option.getValue().getGreen(), option.getValue().getBlue(), 255);
        WalksyLibColor colorG = new WalksyLibColor(option.getValue().getRed(), option.getValue().getGreen(), option.getValue().getBlue(), 0);
        Renderer.fillRoundedRectGradient(context, COLOR_PICKER_STARTX - 32, getY() + 21, 11, opacityHeight - 4, 2, color.getRGB(), colorG.getRGB());

        int opacitySliderX = COLOR_PICKER_STARTX - 34;
        int opacitySliderY = getY() + 21;
        int opacitySliderHeight = opacityHeight - 4;

        int rawOpacityThumbY = opacitySliderY + (int) ((1f - (option.getValue().getAlpha() / 255f)) * opacitySliderHeight) - 2;
        int opacityThumbY = Math.max(opacitySliderY, Math.min(rawOpacityThumbY - 1, opacitySliderY + opacitySliderHeight - 2));

        drawSliderThumb(context, opacitySliderX + 3, opacityThumbY);
    }

    public void drawHueSlider(DrawContext context)
    {
        int hueHeight = getHeight() - 30 + 8;
        Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 17, getY() + 19, 15, hueHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 16, getY() + 20, 13, hueHeight - 2, 2, 1, isHoveringHueSlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer.drawRoundedHueSlider(context, COLOR_PICKER_STARTX - 15, getY() + 21, 11, hueHeight - 4, 2);

        int hueSliderX = COLOR_PICKER_STARTX - 17;
        int hueSliderY = getY() + 21;
        int hueSliderHeight = hueHeight - 4;

        int rawHueThumbY = hueSliderY + (int) ((1f - option.getValue().getHue()) * hueSliderHeight) - 2;
        int hueThumbY = Math.max(hueSliderY, Math.min(rawHueThumbY - 1, hueSliderY + hueSliderHeight - 2));

        drawSliderThumb(context, hueSliderX + 3, hueThumbY);
    }

    public void drawSaturationBox(DrawContext context)
    {
        Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX, getY() + 19, getWidth() - COLOR_PICKER_STARTX + 9, getHeight() - 28 + 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX + 1, getY() + 20, getWidth() - COLOR_PICKER_STARTX + 7, getHeight() - 28 + 6 - 2, 2, 1, isHoveringSaturationValueBox(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer.drawHueSaturationValueBox(context, COLOR_PICKER_STARTX + 2, getY() + 21, getWidth() - COLOR_PICKER_STARTX + 5, getHeight() - 28 + 6 - 4, 2, option.getValue().getHue());

        int boxX = COLOR_PICKER_STARTX;
        int boxY = getY() + 21;
        int boxWidth = getWidth() - COLOR_PICKER_STARTX + 9;
        int boxHeight = getHeight() - 28 + 6 - 4;
        int rawThumbX = boxX + (int) (option.getValue().getSaturation() * boxWidth) + 3;
        int rawThumbY = boxY + (int) ((1f - option.getValue().getBrightness()) * boxHeight) + 1;

        int thumbX = Math.max(boxX + 1, Math.min(rawThumbX - 6, boxX + boxWidth - 8));
        int thumbY = Math.max(boxY, Math.min(rawThumbY - 4, boxY + boxHeight - 6));

        drawSatThumb(context, thumbX, thumbY);
    }

    public boolean isHoveringHueSlider(double mouseX, double mouseY) {
        int x = COLOR_PICKER_STARTX - 17;
        int y = getY() + 19;
        int width = 15;
        int height = getHeight() - 30 + 8;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isHoveringSaturationValueBox(double mouseX, double mouseY) {
        int x = COLOR_PICKER_STARTX;
        int y = getY() + 19;
        int width = getWidth() - COLOR_PICKER_STARTX + 9;
        int height = getHeight() - 28 + 6;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isHoveringOpacitySlider(double mouseX, double mouseY) {
        int x = COLOR_PICKER_STARTX - 34;
        int y = getY() + 19;
        int width = 15;
        int height = getHeight() - 30 + 8;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean isHoveringRainbowButton(double mouseX, double mouseY) {
        int x = COLOR_PICKER_STARTX - 54;
        int y = getY() + 19;
        int width = 18;
        int height = (getHeight() - 85) + 3;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }


    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        super.onMouseClick(mouseX, mouseY, button);
        this.chromaSpeedSlider.onClick((int) mouseX, (int) mouseY, button);
        this.pulseSpeedSlider.onClick((int) mouseX, (int) mouseY, button);
        this.chromaButton.onClick(mouseX, mouseY);
        this.pulseButton.onClick(mouseX, mouseY);

        if (button != 0) return;

        if (open) {
            if (isHoveringHueSlider(mouseX, mouseY)) {
                activeDrag = DragTarget.HUE_SLIDER;
                handleHueSliderClick(mouseY);
                onChange();
                return;
            }
            if (isHoveringSaturationValueBox(mouseX, mouseY)) {
                activeDrag = DragTarget.SATURATION_VALUE_BOX;
                handleSatValBoxClick(mouseX, mouseY);
                onChange();
                return;
            }
            if (isHoveringOpacitySlider(mouseX, mouseY)) {
                activeDrag = DragTarget.OPACITY_SLIDER;
                handleOpacitySliderClick(mouseY);
                onChange();
                return;
            }
        }
    }

    @Override
    protected void onOpen(boolean prev) {

    }

    @Override
    public boolean isHovered() {
        return mouseX >= getX() && mouseX < (getX() + getWidth() - 6)
                && mouseY >= getY() && mouseY < (getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT) && isVisible();
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        activeDrag = DragTarget.NONE;
    }

    @Override
    public void onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.chromaSpeedSlider.onDrag((int) mouseX);
        this.pulseSpeedSlider.onDrag((int) mouseX);
        if (button != 0) return;

        switch (activeDrag) {
            case HUE_SLIDER -> handleHueSliderDrag(mouseY);
            case SATURATION_VALUE_BOX -> handleSatValBoxDrag(mouseX, mouseY);
            case OPACITY_SLIDER -> handleOpacitySliderDrag(mouseY);
            default -> {
            }
        }
        onChange();
    }


    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.chromaSpeedSlider.release();
        this.pulseSpeedSlider.release();
    }

    @Override
    public void onWidgetUpdate() {
        this.chromaSpeedSlider.setPos(new Point(getX() + 28, getY() + 23 + 10));
        this.pulseSpeedSlider.setPos(new Point(getX() + 28, getY() + 49 + 10));
        this.chromaButton.setX(getX() + 5);
        this.chromaButton.setY(getY() + 29);
        this.pulseButton.setX(getX() + 5);
        this.pulseButton.setY(getY() + 55);

        int width = ((getX() + getWidth()) / 2) -  32 - 60;
        this.chromaSpeedSlider.setWidth(width);
        this.pulseSpeedSlider.setWidth(width);
    }

    @Override
    public <V> void onThirdPartyChange(V value) {
        super.onThirdPartyChange(value);
        this.pulseSpeedSlider.setValue(this.option.getValue().getPulseSpeed());
        this.chromaSpeedSlider.setValue(this.option.getValue().getRainbowSpeed());
    }

    private void handleHueSliderClick(double mouseY) {
        float newHue = 1f - (float) ((mouseY - (getY() + 20)) / (getHeight() - 30 + 6));
        option.getValue().setHue(MathHelper.clamp(newHue, 0f, 1f));
        updateColor();
    }


    private void handleHueSliderDrag(double mouseY) {
        handleHueSliderClick(mouseY);
    }

    private void handleSatValBoxClick(double mouseX, double mouseY) {
        int boxX = COLOR_PICKER_STARTX;
        int boxY = getY() + 20;
        int boxWidth = getWidth() - COLOR_PICKER_STARTX + 9;
        int boxHeight = getHeight() - 29 + 6;

        float newSaturation = (float) ((mouseX - boxX) / (double) boxWidth);
        float newBrightness = 1f - (float) ((mouseY - boxY) / (double) boxHeight);

        if(mouseX < boxX) {
            option.getValue().setSaturation(0f);
        } else if (mouseX > boxX + boxWidth) {
            option.getValue().setSaturation(1f);
        } else {
            option.getValue().setSaturation(MathHelper.clamp(newSaturation, 0f, 1f));
        }
        if(mouseY < boxY) {
            option.getValue().setBrightness(1f);
        } else if (mouseY > boxY + boxHeight) {
            option.getValue().setBrightness(0f);
        } else {
            option.getValue().setBrightness(MathHelper.clamp(newBrightness, 0f, 1f));
        }

        updateColor();
    }

    private void handleSatValBoxDrag(double mouseX, double mouseY) {
        handleSatValBoxClick(mouseX, mouseY);
    }

    private void handleOpacitySliderClick(double mouseY) {
        int sliderY = getY() + 20;
        int sliderHeight = getHeight() - 30 + 6;

        float newOpacity = 1f - (float) ((mouseY - sliderY) / (double) sliderHeight);
        option.getValue().setAlpha((int) (MathHelper.clamp(newOpacity, 0f, 1f) * 255));

        updateColor();
    }

    private void handleOpacitySliderDrag(double mouseY) {
        handleOpacitySliderClick(mouseY);
    }

    private void updateColor() {
        int rgb = WalksyLibColor.HSBtoRGB(option.getValue().getHue(), option.getValue().getSaturation(), option.getValue().getBrightness());
        WalksyLibColor updated = new WalksyLibColor(
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF,
                option.getValue().getAlpha()
        );
        updated.setAdditions(this.option.getValue().getAdditions());
        option.setValue(updated);
    }
}