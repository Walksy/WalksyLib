package main.walksy.lib.core.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
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
    private final Option<Color> option;

    public int COLOR_PICKER_STARTX;

    private final Identifier TRANSPARENT_BACKGROUND = Identifier.of("walksylib", "gui/widget/transparent.png");
    private final Identifier RAINBOW_ICON = Identifier.of("walksylib", "gui/widget/rainbow.png");
    private final Identifier PULSE_ICON = Identifier.of("walksylib", "gui/widget/pulse.png");

    private final int MAX_RAINBOW_SPEED = 20;
    private final int MIN_RAINBOW_SPEED = 1;

    private final int MAX_PULSE_SPEED = 20;
    private final int MIN_PULSE_SPEED = 1;

    private enum DragTarget {
        NONE,
        HUE_SLIDER,
        SATURATION_VALUE_BOX,
        OPACITY_SLIDER,
        RAINBOW,
        PULSE
    }

    private DragTarget activeDrag = DragTarget.NONE;

    public ColorWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<Color> option) {
        super(parent, screen, option, x, y, width, height, option.getName(), WalksyLibScreenManager.Globals.OPTION_HEIGHT * 5);
        this.option = option;
        Color initial = option.getValue();
        float[] hsb = Color.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
        option.setHue(hsb[0]);
        option.setSaturation(hsb[1]);
        option.setBrightness(hsb[2]);
        option.setAlpha(initial.getAlpha());
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
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
                Color.LIGHT_GRAY.getRGB()
        );

        if (!open) {
            context.drawVerticalLine(getX() + getWidth() - 38, getY(), getY() + WalksyLibScreenManager.Globals.OPTION_HEIGHT - 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        }

        if (open) {
            int heightAdjusted = getHeight() - 28 + 6;
            int hueHeight = getHeight() - 30 + 8;
            int opacityHeight = getHeight() - 30 + 8;
            int buttonHeight = (getHeight() - 85) + 3;

            //Chroma Box
            int chromaBoxX = getX() + 5;
            int chromaBoxWidth = (getWidth() / 2) - 68;
            int chromaBoxHeight = heightAdjusted - 45;

            Renderer.fillRoundedRectOutline(context, chromaBoxX, getY() + 19, chromaBoxWidth, chromaBoxHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
            Renderer.fillRoundedRectOutline(context, chromaBoxX + 1, getY() + 20, chromaBoxWidth - 2, chromaBoxHeight - 2, 2, 1, isHoveringRainbowSlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

            int centerX = chromaBoxX + chromaBoxWidth / 2;
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "Rainbow Speed - " + option.getRainbowSpeed(), centerX, getY() + 26, -1);

            //(thumb)
            int sliderX = chromaBoxX + 10;
            int sliderWidth = chromaBoxWidth - 20;
            int thumbWidth = 12;
            int speed = option.getRainbowSpeed();

            float normalized = MathHelper.clamp((float) (speed - MIN_RAINBOW_SPEED) / (MAX_RAINBOW_SPEED - MIN_RAINBOW_SPEED), 0f, 1f);

            int rmaxThumbX = sliderX + sliderWidth - thumbWidth;
            int rthumbX = sliderX + (int)(normalized * (rmaxThumbX - sliderX));

            int thumbColor = isHoveringRainbowSlider(mouseX, mouseY)
                    ? MainColors.OUTLINE_WHITE_HOVERED.getRGB()
                    : MainColors.OUTLINE_WHITE.getRGB();

            Renderer.fillRoundedRect(
                    context,
                    rthumbX,
                    getY() + 40,
                    thumbWidth,
                    6,
                    2,
                    thumbColor
            );


            Renderer.fillRoundedRectOutline(
                    context,
                    sliderX,
                    getY() + 40,
                    sliderWidth,
                    6,
                    1,
                    1,
                    new Color(
                            MainColors.OUTLINE_BLACK.getRed(),
                            MainColors.OUTLINE_BLACK.getGreen(),
                            MainColors.OUTLINE_BLACK.getBlue(),
                            255
                    ).getRGB()
            );


            //Pulse Box
            int pulseBoxY = getY() + 19 + chromaBoxHeight + 5;
            int pulseBoxHeight = 40;

            Renderer.fillRoundedRectOutline(context, chromaBoxX, pulseBoxY, chromaBoxWidth, pulseBoxHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
            Renderer.fillRoundedRectOutline(context, chromaBoxX + 1, pulseBoxY + 1, chromaBoxWidth - 2, pulseBoxHeight - 2, 2, 1, isHoveringPulseSlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "(Bright) Pulse Speed - " + option.getPulseSpeed(), centerX, pulseBoxY + 6, -1);

            float normalizedPulse = MathHelper.clamp((float)(option.getPulseSpeed() - MIN_PULSE_SPEED) / (MAX_PULSE_SPEED - MIN_PULSE_SPEED), 0f, 1f);
            int pmaxThumbX = sliderX + sliderWidth - thumbWidth;
            int pulseThumbX = sliderX + (int)(normalizedPulse * (pmaxThumbX - sliderX));

            int pulseThumbColor = isHoveringPulseSlider(mouseX, mouseY)
                    ? MainColors.OUTLINE_WHITE_HOVERED.getRGB()
                    : MainColors.OUTLINE_WHITE.getRGB();

            Renderer.fillRoundedRect(
                    context,
                    pulseThumbX,
                    pulseBoxY + 22,
                    thumbWidth,
                    6,
                    2,
                    pulseThumbColor
            );

            Renderer.fillRoundedRectOutline(
                    context,
                    sliderX,
                    pulseBoxY + 22,
                    sliderWidth,
                    6,
                    1,
                    1,
                    new Color(
                            MainColors.OUTLINE_BLACK.getRed(),
                            MainColors.OUTLINE_BLACK.getGreen(),
                            MainColors.OUTLINE_BLACK.getBlue(),
                            255
                    ).getRGB()
            );

            // Saturation/Value box
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX, getY() + 19, getWidth() - COLOR_PICKER_STARTX + 9, heightAdjusted, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX + 1, getY() + 20, getWidth() - COLOR_PICKER_STARTX + 7, heightAdjusted - 2, 2, 1, isHoveringSaturationValueBox(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
            Renderer.drawHueSaturationValueBox(context, COLOR_PICKER_STARTX + 2, getY() + 21, getWidth() - COLOR_PICKER_STARTX + 5, heightAdjusted - 4, 2, option.getHue());

            int boxX = COLOR_PICKER_STARTX;
            int boxY = getY() + 21;
            int boxWidth = getWidth() - COLOR_PICKER_STARTX + 9;
            int boxHeight = heightAdjusted - 4;

            int rawThumbX = boxX + (int) (option.getSaturation() * boxWidth) + 3;
            int rawThumbY = boxY + (int) ((1f - option.getBrightness()) * boxHeight) + 1;

            int thumbX = Math.max(boxX + 1, Math.min(rawThumbX - 6, boxX + boxWidth - 8));
            int thumbY = Math.max(boxY, Math.min(rawThumbY - 4, boxY + boxHeight - 6));

            drawSatThumb(context, thumbX, thumbY);


            // Hue slider
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 17, getY() + 19, 15, hueHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 16, getY() + 20, 13, hueHeight - 2, 2, 1, isHoveringHueSlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
            Renderer.drawRoundedHueSlider(context, COLOR_PICKER_STARTX - 15, getY() + 21, 11, hueHeight - 4, 2);

            int hueSliderX = COLOR_PICKER_STARTX - 17;
            int hueSliderY = getY() + 21;
            int hueSliderHeight = hueHeight - 4;

            int rawHueThumbY = hueSliderY + (int) ((1f - option.getHue()) * hueSliderHeight) - 2;
            int hueThumbY = Math.max(hueSliderY, Math.min(rawHueThumbY - 1, hueSliderY + hueSliderHeight - 2));

            drawSliderThumb(context, hueSliderX + 3, hueThumbY);


            // Opacity slider
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 34, getY() + 19, 15, opacityHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 33, getY() + 20, 13, opacityHeight - 2, 2, 1, isHoveringOpacitySlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
            Renderer.drawRoundedTexture(context, RenderLayer::getGuiTextured, TRANSPARENT_BACKGROUND, COLOR_PICKER_STARTX - 32, getY() + 21, 11, opacityHeight - 4, 2, 4, 4);
            Color color = new Color(option.getValue().getRed(), option.getValue().getGreen(), option.getValue().getBlue(), 255);
            Color colorG = new Color(option.getValue().getRed(), option.getValue().getGreen(), option.getValue().getBlue(), 0);
            Renderer.fillRoundedRectGradient(context, COLOR_PICKER_STARTX - 32, getY() + 21, 11, opacityHeight - 4, 2, color.getRGB(), colorG.getRGB());

            int opacitySliderX = COLOR_PICKER_STARTX - 34;
            int opacitySliderY = getY() + 21;
            int opacitySliderHeight = opacityHeight - 4;

            int rawOpacityThumbY = opacitySliderY + (int) ((1f - (option.getAlpha() / 255f)) * opacitySliderHeight) - 2;
            int opacityThumbY = Math.max(opacitySliderY, Math.min(rawOpacityThumbY - 1, opacitySliderY + opacitySliderHeight - 2));

            drawSliderThumb(context, opacitySliderX + 3, opacityThumbY);



            //Pulse Buttons
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 54, getY() + 19 + 38, 19, buttonHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 53, getY() + 20 + 38, 17, buttonHeight - 2, 2, 1, (isHoveringSinePulseButton(mouseX, mouseY) || option.getPulseValue() == Option.PulseValue.SINE) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
            int back = new Color(0, 0, 0, 100).getRGB();
            Renderer.fillRoundedRect(context, COLOR_PICKER_STARTX - 52, getY() + 21 + 38, 15, buttonHeight - 4, 2, back);
            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    PULSE_ICON,
                    COLOR_PICKER_STARTX - 52 + (14 - 16) / 2,
                    (getY() + 20 + (buttonHeight - 4 - 16) / 2) + 38,
                    0, 0, 16, 16, 16, 16, option.getPulseValue() == Option.PulseValue.SINE ? -1 : Color.GRAY.getRGB()
            );

            //Rainbow Buttons
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 54, getY() + 19, 19, buttonHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
            Renderer.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 53, getY() + 20, 17, buttonHeight - 2, 2, 1, (isHoveringRainbowButton(mouseX, mouseY) || option.isRainbow()) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
            Renderer.fillRoundedRect(context, COLOR_PICKER_STARTX - 52, getY() + 21, 15, buttonHeight - 4, 2, back);
            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    RAINBOW_ICON,
                    COLOR_PICKER_STARTX - 52 + (14 - 16) / 2,
                    getY() + 20 + (buttonHeight - 4 - 16) / 2,
                    0, 0, 16, 16, 16, 16, option.isRainbow() ? -1 : Color.GRAY.getRGB()
            );
        }
    }

    public void drawSatThumb(DrawContext context, int x, int y)
    {
        Renderer.fillRoundedRectOutline(context, x, y, 6, 6, 1, 1, Color.BLACK.getRGB());
        Renderer.fillRoundedRectOutline(context, x + 1, y + 1, 4, 4, 1, 1, MainColors.OUTLINE_WHITE.getRGB());
    }

    public void drawSliderThumb(DrawContext context, int x, int y)
    {
        Renderer.fillRoundedRectOutline(context, x, y, 9, 3, 1, 1, Color.BLACK.getRGB());
        //Renderer.fillRoundedRectOutline(context, x + 1, y + 1, 4, 2, 1, 1, MainColors.OUTLINE_WHITE.getRGB());
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

    public boolean isHoveringSinePulseButton(double mouseX, double mouseY) {
        int x = COLOR_PICKER_STARTX - 54;
        int y = getY() + 19 + 38;
        int width = 18;
        int height = (getHeight() - 85) + 3;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isHoveringRainbowSlider(double mouseX, double mouseY) {
        int heightAdjusted = getHeight() - 28 + 6;
        int chromaBoxX = getX() + 5;
        int chromaBoxWidth = (getWidth() / 2) - 68;
        int chromaBoxHeight = heightAdjusted - 45;
        int chromaBoxY = getY() + 19;

        return mouseX >= chromaBoxX && mouseX <= chromaBoxX + chromaBoxWidth &&
                mouseY >= chromaBoxY && mouseY <= chromaBoxY + chromaBoxHeight;
    }

    public boolean isHoveringPulseSlider(double mouseX, double mouseY) {
        int heightAdjusted = getHeight() - 28 + 6;
        int chromaBoxHeight = heightAdjusted - 45;
        int chromaBoxX = getX() + 5;
        int chromaBoxWidth = (getWidth() / 2) - 68;
        int pulseBoxY = getY() + 19 + chromaBoxHeight + 5;
        int pulseBoxHeight = 40;

        return mouseX >= chromaBoxX && mouseX <= chromaBoxX + chromaBoxWidth &&
                mouseY >= pulseBoxY && mouseY <= pulseBoxY + pulseBoxHeight;
    }


    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        super.onMouseClick(mouseX, mouseY, button);
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
            if (isHoveringRainbowButton(mouseX, mouseY)) {
                handleRainbowButtonClick();
                onChange();
                return;
            }
            if (isHoveringSinePulseButton(mouseX, mouseY)) {
                handleSineButtonClick();
                onChange();
                return;
            }
            if (isHoveringRainbowSlider(mouseX, mouseY))
            {
                activeDrag = DragTarget.RAINBOW;
                handleRainbowSliderClick(mouseX);
                onChange();
                return;
            }
            if (isHoveringPulseSlider(mouseX, mouseY)) {
                activeDrag = DragTarget.PULSE;
                handlePulseSliderClick(mouseX);
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
        if (button != 0) return;

        switch (activeDrag) {
            case HUE_SLIDER -> handleHueSliderDrag(mouseY);
            case SATURATION_VALUE_BOX -> handleSatValBoxDrag(mouseX, mouseY);
            case OPACITY_SLIDER -> handleOpacitySliderDrag(mouseY);
            case RAINBOW -> handleRainbowSliderClick(mouseX);
            case PULSE -> handlePulseSliderClick(mouseX);
            default -> {
            }
        }
        onChange();
    }

    private boolean rToolTip = false; //this is stupid

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        if (isHoveringRainbowButton(mouseX, mouseY))
        {
            //TODO
            if (!rToolTip) {
                //this.setTooltip(Text.of("Toggle Rainbow Option"));
                rToolTip = true;
            }
        } else {
            rToolTip = false;
        }
    }


    private void handleHueSliderClick(double mouseY) {
        float newHue = 1f - (float) ((mouseY - (getY() + 20)) / (getHeight() - 30 + 6));
        option.setHue(MathHelper.clamp(newHue, 0f, 1f));
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
            option.setSaturation(0f);
        } else if (mouseX > boxX + boxWidth) {
            option.setSaturation(1f);
        } else {
            option.setSaturation(MathHelper.clamp(newSaturation, 0f, 1f));
        }
        if(mouseY < boxY) {
            option.setBrightness(1f);
        } else if (mouseY > boxY + boxHeight) {
            option.setBrightness(0f);
        } else {
            option.setBrightness(MathHelper.clamp(newBrightness, 0f, 1f));
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
        option.setAlpha((int) (MathHelper.clamp(newOpacity, 0f, 1f) * 255));

        updateColor();
    }

    private void handleOpacitySliderDrag(double mouseY) {
        handleOpacitySliderClick(mouseY);
    }

    private void handleRainbowButtonClick() {
        option.setRainbow(!option.isRainbow());
    }

    private void handleSineButtonClick() {
        if (option.getPulseValue() != Option.PulseValue.SINE) {
            option.setPulseValue(Option.PulseValue.SINE);
        } else {
            option.setPulseValue(null);
        }
    }

    private void handleRainbowSliderClick(double mouseX) {
        int chromaBoxX = getX() + 5;
        int chromaBoxWidth = (getWidth() / 2) - 68;

        int centerX = chromaBoxX + chromaBoxWidth / 2;
        int sliderX = centerX - 30 - 40;
        int sliderWidth = 130;

        double clampedX = MathHelper.clamp(mouseX, sliderX, sliderX + sliderWidth);
        double relativePos = (clampedX - sliderX) / sliderWidth;

        int rainbowSpeed = MIN_RAINBOW_SPEED + (int)(relativePos * (MAX_RAINBOW_SPEED - MIN_RAINBOW_SPEED));
        option.setRainbowSpeed(rainbowSpeed);
    }


    private void handlePulseSliderClick(double mouseX) {
        int chromaBoxX = getX() + 5;
        int chromaBoxWidth = (getWidth() / 2) - 68;

        int centerX = chromaBoxX + chromaBoxWidth / 2;
        int sliderX = centerX - 30 - 40;
        int sliderWidth = 130;

        double clampedX = MathHelper.clamp(mouseX, sliderX, sliderX + sliderWidth);
        double relativePos = (clampedX - sliderX) / sliderWidth;

        int pulseSpeed = MIN_PULSE_SPEED + (int)(relativePos * (MAX_PULSE_SPEED - MIN_PULSE_SPEED));
        option.setPulseSpeed(pulseSpeed);
    }


    private void updateColor() {
        int rgb = Color.HSBtoRGB(option.getHue(), option.getSaturation(), option.getBrightness());
        Color updated = new Color(
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF,
                option.getAlpha()
        );
        option.setValue(updated);
    }
}
