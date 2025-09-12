package main.walksy.lib.core.config.local.options.type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PixelGridAnimation {
    private final List<PixelGrid> frames = new ArrayList<>();
    private int currentFrame = 0;
    private int tickCounter = 0;
    private int animationSpeed = 10;

    private float relativeX = 0.5f;
    private float relativeY = 0.5f;

    public PixelGridAnimation(PixelGrid... grids) {
        this(Arrays.asList(grids));
    }

    public PixelGridAnimation(List<PixelGrid> grids) {
        frames.addAll(grids);
    }

    public PixelGridAnimation(PixelGridAnimation original, PixelGrid replacement, int index) {
        for (int i = 0; i < original.frames.size(); i++) {
            frames.add(i == index - 1 ? replacement : original.frames.get(i));
        }
        this.relativeX = original.relativeX;
        this.relativeY = original.relativeY;
    }

    public static PixelGridAnimation replace(PixelGridAnimation original, PixelGrid replacement, int index) {
        PixelGridAnimation result = new PixelGridAnimation(original, replacement, index);
        result.setCurrentFrame(original.currentFrame);
        result.setAnimationSpeed(original.animationSpeed);
        return result;
    }

    public void resetAnimation() {
        currentFrame = 0;
        tickCounter = 0;
    }

    public void render(DrawContext context) {
        Point pos = getAbsolutePosition();
        this.getCurrentFrame().render(context, pos.x, pos.y);
    }

    public void render(DrawContext context, int x, int y) {
        this.getCurrentFrame().render(context, x, y);
    }

    public void tick() {
        if (frames.isEmpty() || animationSpeed <= 0) return;

        tickCounter++;
        int frameDelay = Math.max(1, 21 - animationSpeed);
        if (tickCounter >= frameDelay) {
            tickCounter = 0;
            currentFrame = (currentFrame + 1) % frames.size();
        }
    }

    public void setAnimationSpeed(int speed) {
        this.animationSpeed = MathHelper.clamp(speed, 1, 100);
    }

    public int getAnimationSpeed() {
        return this.animationSpeed;
    }

    public PixelGrid getCurrentFrame() {
        return frames.isEmpty() ? null : frames.get(currentFrame);
    }

    public List<PixelGrid> getFrames() {
        return frames;
    }

    public PixelGrid getFrame(int index) {
        return frames.get(index - 1);
    }

    public void setFrame(int index, PixelGrid grid) {
        frames.set(index - 1, grid);
    }

    public void addFrame(PixelGrid grid) {
        frames.add(grid);
    }

    public void setCurrentFrame(int index) {
        this.currentFrame = MathHelper.clamp(index, 0, frames.size() - 1);
    }

    public Point getAbsolutePosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int x = Math.round(screenWidth * relativeX);
        int y = Math.round(screenHeight * relativeY);
        return new Point(x, y);
    }

    public void setPosition(int absoluteX, int absoluteY) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        if (screenWidth == 0 || screenHeight == 0) return;

        this.relativeX = MathHelper.clamp((float) absoluteX / screenWidth, 0.0f, 1.0f);
        this.relativeY = MathHelper.clamp((float) absoluteY / screenHeight, 0.0f, 1.0f);
    }

    public void setRelativePosition(float x, float y)
    {
        this.relativeX = x;
        this.relativeY = y;
    }

    public float getRelativeX()
    {
        return this.relativeX;
    }

    public float getRelativeY()
    {
        return this.relativeY;
    }

    public PixelGridAnimation copy() {
        List<PixelGrid> copiedFrames = frames.stream().map(PixelGrid::copy).toList();
        PixelGridAnimation copy = new PixelGridAnimation(copiedFrames);
        copy.currentFrame = this.currentFrame;
        copy.animationSpeed = this.animationSpeed;
        copy.relativeX = this.relativeX;
        copy.relativeY = this.relativeY;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PixelGridAnimation other)) return false;
        if (this.animationSpeed != other.animationSpeed) return false;
        if (this.relativeX != other.relativeX || this.relativeY != other.relativeY) return false;
        if (this.frames.size() != other.frames.size()) return false;

        for (int i = 0; i < frames.size(); i++) {
            if (!this.frames.get(i).equals(other.frames.get(i))) return false;
        }

        return true;
    }
}
