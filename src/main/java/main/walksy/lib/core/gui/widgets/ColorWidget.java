package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.sub.SliderSubWidget;
import main.walksy.lib.core.gui.widgets.sub.TextboxSubWidget;
import main.walksy.lib.core.gui.widgets.sub.adaptor.IntSliderAdapter;
import main.walksy.lib.core.renderer.Renderer2D;
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

    public ColorWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<WalksyLibColor> option) {
        super(parent, screen, option, x, y, width, height, option.getName(), ScreenGlobals.OPTION_HEIGHT * 5);
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
        this.chromaSpeedSlider = new SliderSubWidget<>(x + 28, y + 23 + 10, COLOR_PICKER_STARTX - 32 - 60, ScreenGlobals.OPTION_HEIGHT - 12, new IntSliderAdapter(1, 20, this.option.getValue().getRainbowSpeed()), this.option.getValue().getRainbowSpeed(), rainbowSpeed -> {
            this.option.getValue().setRainbowSpeed(rainbowSpeed);
            this.updateThumbTargets(false);
        }, true);
        this.pulseSpeedSlider = new SliderSubWidget<>(x + 28, y + 49 + 10, COLOR_PICKER_STARTX - 32 - 60, ScreenGlobals.OPTION_HEIGHT - 12, new IntSliderAdapter(1, 20, this.option.getValue().getPulseSpeed()), this.option.getValue().getPulseSpeed(), pulseSpeed ->
        {
            this.option.getValue().setPulseSpeed(pulseSpeed);
            this.updateThumbTargets(false);
        }, true);
        String initialHex = String.format("#%02X%02X%02X%02X",
                initial.getRed(), initial.getGreen(), initial.getBlue(), initial.getAlpha());
        this.hexInput = new TextboxSubWidget(screen, 0, 0, 78, 78, 14, initialHex, str -> {}, false);
        this.hexInput.setOnFocusLost(() -> {
            String hexStr = this.hexInput.getText();
            String clean = hexStr.startsWith("#") ? hexStr.substring(1) : hexStr;
            if (clean.length() != 6 && clean.length() != 8) {
                syncHexInput();
                return;
            }
            try {
                int r = Integer.parseInt(clean.substring(0, 2), 16);
                int g = Integer.parseInt(clean.substring(2, 4), 16);
                int b = Integer.parseInt(clean.substring(4, 6), 16);
                int a = clean.length() == 8
                        ? Integer.parseInt(clean.substring(6, 8), 16)
                        : option.getValue().getAlpha();
                float[] parsedHsb = WalksyLibColor.RGBtoHSB(r, g, b, null);
                option.getValue().setHue(parsedHsb[0]);
                option.getValue().setSaturation(parsedHsb[1]);
                option.getValue().setBrightness(parsedHsb[2]);
                option.getValue().setAlpha(a);
                WalksyLibColor updated = new WalksyLibColor(r, g, b, a);
                updated.setAdditions(option.getValue().getAdditions());
                option.setValue(updated);
                updateThumbTargets(false);
                syncHexInput();
            } catch (NumberFormatException ignored) {
                syncHexInput();
            }
        });
    }

    @Override
    public void draw(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.draw(context, mouseX, mouseY, delta);
        //if the option value object changes (via a reset & object copying etc...), the slider's consumer will point to the wrong object
        this.pulseSpeedSlider.setOnChange(this.option.getValue()::setPulseSpeed);
        this.chromaSpeedSlider.setOnChange(this.option.getValue()::setRainbowSpeed);
        COLOR_PICKER_STARTX = (getX() + getWidth()) / 2;
        int baseHeight = ScreenGlobals.OPTION_HEIGHT;
        satThumbXAnim.update(delta);
        satThumbYAnim.update(delta);
        hueThumbYAnim.update(delta);
        opacityThumbYAnim.update(delta);
        Renderer2D.drawRoundedTexture(context, RenderPipelines.GUI_TEXTURED, TRANSPARENT_BACKGROUND, getWidth() - 15, getY() + 4, 23, baseHeight - 8, 2, 4, 4);
        Renderer2D.fillRoundedRectOutline(context, getWidth() - 16, getY() + 3, 25, baseHeight - 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer2D.fillRoundedRect(context, getWidth() - 15, getY() + 4, 23, baseHeight - 8, 2, option.getValue().getRGB());

        hexInput.render(context, mouseX, mouseY, delta);

        /*
        context.text(
                Minecraft.getInstance().font,
                String.format("#%02X%02X%02X%02X",
                        option.getValue().getRed(),
                        option.getValue().getGreen(),
                        option.getValue().getBlue(),
                        option.getValue().getAlpha()
                ),
                getX() + getWidth() - 98,
                getTextYCentered() + 1,
                java.awt.Color.LIGHT_GRAY.getRGB(),
                true
        );
         */

        if (this.fullyClosed()) {
            context.verticalLine(getX() + getWidth() - 38, getY(), getY() + ScreenGlobals.OPTION_HEIGHT - 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        } else if (this.getCurrentHeight() > 34) { // Renderer2D::drawRoundedRec methods freak out if their given width and/or height is too low, therefore we stop rendering them early before the animation finishes
            drawHueSlider(context);
            drawSaturationBox(context);
            drawOpacitySlider(context);
            this.chromaButton.overrideHover = this.option.getValue().isRainbow();
            this.pulseButton.overrideHover = this.option.getValue().isPulse();
            this.chromaButton.extractWidgetRenderState(context, mouseX, mouseY, delta);
            this.pulseButton.extractWidgetRenderState(context, mouseX, mouseY, delta);
            this.chromaSpeedSlider.render(context, mouseX, mouseY, delta);
            this.pulseSpeedSlider.render(context, mouseX, mouseY, delta);

            context.text(
                    Minecraft.getInstance().font,
                    "Rainbow Speed",
                    getX() + 32,
                    getY() + 23,
                    Color.LIGHT_GRAY.getRGB(),
                    true
            );

            context.text(
                    Minecraft.getInstance().font,
                    "Pulse Speed",
                    getX() + 32,
                    getY() + 49,
                    Color.LIGHT_GRAY.getRGB(),
                    true
            );
        }
    }

    public void drawSatThumb(GuiGraphicsExtractor context, int x, int y)
    {
        Renderer2D.fillRoundedRectOutline(context, x, y, 6, 6, 1, 1, Color.BLACK.getRGB());
        Renderer2D.fillRoundedRectOutline(context, x + 1, y + 1, 4, 4, 1, 1, MainColors.OUTLINE_WHITE.getRGB());
    }

    public void drawSliderThumb(GuiGraphicsExtractor context, int x, int y)
    {
        Renderer2D.fillRoundedRectOutline(context, x, y, 9, 3, 1, 1, Color.BLACK.getRGB());
    }

    public void drawOpacitySlider(GuiGraphicsExtractor  context)
    {
        int opacityHeight = getHeight() - 30 + 8;
        Renderer2D.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 34, getY() + 19, 15, opacityHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer2D.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 33, getY() + 20, 13, opacityHeight - 2, 2, 1, isHoveringOpacitySlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer2D.drawRoundedTexture(context, RenderPipelines.GUI_TEXTURED, TRANSPARENT_BACKGROUND, COLOR_PICKER_STARTX - 32, getY() + 21, 11, opacityHeight - 4, 2, 4, 4);
        WalksyLibColor color = new WalksyLibColor(option.getValue().getRed(), option.getValue().getGreen(), option.getValue().getBlue(), 255);
        WalksyLibColor colorG = new WalksyLibColor(option.getValue().getRed(), option.getValue().getGreen(), option.getValue().getBlue(), 0);
        Renderer2D.fillRoundedRectGradient(context, COLOR_PICKER_STARTX - 32, getY() + 21, 11, opacityHeight - 4, 2, color.getRGB(), colorG.getRGB());

        int opacitySliderX = COLOR_PICKER_STARTX - 34;
        drawSliderThumb(context, opacitySliderX + 3, (int) opacityThumbYAnim.getCurrentValue() + 1);
    }

    public void drawHueSlider(GuiGraphicsExtractor context)
    {
        int hueHeight = getHeight() - 30 + 8;
        Renderer2D.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 17, getY() + 19, 15, hueHeight, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer2D.fillRoundedRectOutline(context, COLOR_PICKER_STARTX - 16, getY() + 20, 13, hueHeight - 2, 2, 1, isHoveringHueSlider(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer2D.drawRoundedHueSlider(context, COLOR_PICKER_STARTX - 15, getY() + 21, 11, hueHeight - 4, 2);

        int hueSliderX = COLOR_PICKER_STARTX - 17;

        drawSliderThumb(context, hueSliderX + 3, (int) hueThumbYAnim.getCurrentValue() + 1);
    }

    public void drawSaturationBox(GuiGraphicsExtractor context)
    {
        Renderer2D.fillRoundedRectOutline(context, COLOR_PICKER_STARTX, getY() + 19, getWidth() - COLOR_PICKER_STARTX + 9, getHeight() - 28 + 6, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer2D.fillRoundedRectOutline(context, COLOR_PICKER_STARTX + 1, getY() + 20, getWidth() - COLOR_PICKER_STARTX + 7, getHeight() - 28 + 6 - 2, 2, 1, isHoveringSaturationValueBox(mouseX, mouseY) ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        Renderer2D.drawHueSaturationValueBox(context, COLOR_PICKER_STARTX + 2, getY() + 21, getWidth() - COLOR_PICKER_STARTX + 5, getHeight() - 28 + 6 - 4, 2, option.getValue().getHue());
        drawSatThumb(context, (int) satThumbXAnim.getCurrentValue() + 1, (int) satThumbYAnim.getCurrentValue());
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
    public void onMouseClick(MouseButtonEvent click, boolean doubled) {
        super.onMouseClick(click, doubled);

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        this.chromaSpeedSlider.onClick(click, doubled);
        this.pulseSpeedSlider.onClick(click, doubled);
        this.chromaButton.onClick(click, doubled);
        this.pulseButton.onClick(click, doubled);
        this.hexInput.onClick(click, doubled);
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
    public void onKeyPress(KeyEvent input) {
        this.hexInput.onKeyPress(input);
    }

    @Override
    public void onCharTyped(CharacterEvent input) {
        this.hexInput.onCharTyped(input);
    }

    @Override
    protected void onOpen(boolean prev) {

    }

    @Override
    public boolean isHovered() {
        return mouseX >= getX() && mouseX < (getX() + getWidth() - 6)
                && mouseY >= getY() && mouseY < (getY() + ScreenGlobals.OPTION_HEIGHT) && isVisible() && isAvailable() && !this.hexInput.hovered;
    }

    @Override
    public void onMouseRelease(MouseButtonEvent click) {
        if (click.button() != 0) return;
        activeDrag = DragTarget.NONE;
    }

    @Override
    public void onMouseDrag(MouseButtonEvent click, double offsetX, double offsetY) {
        this.chromaSpeedSlider.onDrag((int) mouseX);
        this.pulseSpeedSlider.onDrag((int) mouseX);
        if (click.button() != 0) return;

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
    public void onChange() {
        super.onChange();
        this.updateThumbTargets(false);
    }

    @Override
    public void onRelease(MouseButtonEvent click) {
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
        this.hexInput.setPos(new Point(getX() + getWidth() - 98, getTextYCentered()- 3));
        int width = ((getX() + getWidth()) / 2) -  32 - 60;
        this.chromaSpeedSlider.setWidth(width);
        this.pulseSpeedSlider.setWidth(width);
        this.updateThumbTargets(true);
    }

    @Override
    public <V> void onThirdPartyChange(V value) {
        super.onThirdPartyChange(value);
        this.pulseSpeedSlider.setValue(this.option.getValue().getPulseSpeed());
        this.chromaSpeedSlider.setValue(this.option.getValue().getRainbowSpeed());
        syncHexInput();
    }

    private void updateThumbTargets(boolean jump) {
        int hueSliderY = getY() + 21;
        int hueSliderHeight = getHeight() - 30 + 8 - 4;
        int rawHueThumbY = hueSliderY + (int) ((1f - option.getValue().getHue()) * hueSliderHeight) - 2;
        int targetHueY = Math.max(hueSliderY - 1, Math.min(rawHueThumbY - 1, hueSliderY + hueSliderHeight - 2));
        int opacitySliderY = getY() + 21;
        int opacitySliderHeight = getHeight() - 30 + 8 - 4;
        int rawOpacityThumbY = opacitySliderY + (int) ((1f - (option.getValue().getAlpha() / 255f)) * opacitySliderHeight) - 2;
        int targetOpacityY = Math.max(opacitySliderY - 1, Math.min(rawOpacityThumbY - 1, opacitySliderY + opacitySliderHeight - 2));
        int boxX = COLOR_PICKER_STARTX;
        int boxY = getY() + 21;
        int boxWidth = getWidth() - COLOR_PICKER_STARTX + 9;
        int boxHeight = getHeight() - 28 + 6 - 4;
        int rawThumbX = boxX + (int) (option.getValue().getSaturation() * boxWidth) + 3;
        int rawThumbY = boxY + (int) ((1f - option.getValue().getBrightness()) * boxHeight) + 1;

        if (jump) {
            hueThumbYAnim.jumpTo(targetHueY);
            satThumbXAnim.jumpTo(Math.max(boxX + 1, Math.min(rawThumbX - 6, boxX + boxWidth - 8)));
            satThumbYAnim.jumpTo(Math.max(boxY, Math.min(rawThumbY - 4, boxY + boxHeight - 6)));
            opacityThumbYAnim.jumpTo(targetOpacityY);
        } else {
            hueThumbYAnim.setTargetValue(targetHueY);
            satThumbXAnim.setTargetValue(Math.max(boxX + 1, Math.min(rawThumbX - 6, boxX + boxWidth - 8)));
            satThumbYAnim.setTargetValue(Math.max(boxY, Math.min(rawThumbY - 4, boxY + boxHeight - 6)));
            opacityThumbYAnim.setTargetValue(targetOpacityY);
        }
    }


    private void handleHueSliderClick(double mouseY) {
        float newHue = 1f - (float) ((mouseY - (getY() + 20)) / (getHeight() - 30 + 6));
        option.getValue().setHue(Mth.clamp(newHue, 0f, 1f));
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
            option.getValue().setSaturation(Mth.clamp(newSaturation, 0f, 1f));
        }
        if(mouseY < boxY) {
            option.getValue().setBrightness(1f);
        } else if (mouseY > boxY + boxHeight) {
            option.getValue().setBrightness(0f);
        } else {
            option.getValue().setBrightness(Mth.clamp(newBrightness, 0f, 1f));
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
        option.getValue().setAlpha((int) (Mth.clamp(newOpacity, 0f, 1f) * 255));

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
        syncHexInput();
    }

    private void syncHexInput() {
        updatingHex = true;
        hexInput.setText(String.format("#%02X%02X%02X%02X",
                option.getValue().getRed(),
                option.getValue().getGreen(),
                option.getValue().getBlue(),
                option.getValue().getAlpha()
        ));
        updatingHex = false;
    }
}