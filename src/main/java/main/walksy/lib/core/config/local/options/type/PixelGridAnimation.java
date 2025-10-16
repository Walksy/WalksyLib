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

    private int offsetX = 0;
    private int offsetY = 0;
    private float size = 1.0f;

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
        this.offsetX = original.offsetX;
        this.offsetY = original.offsetY;
        this.size = original.size;
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
        this.render(context, pos.x, pos.y);
    }

    public void render(DrawContext context, int x, int y) {
        PixelGrid frame = this.getCurrentFrame();
        if (frame != null) {
            context.getMatrices().push();
            context.getMatrices().scale(size, size, 1.0f);
            frame.render(context, (int) (x / size), (int) (y / size));
            context.getMatrices().pop();
        }
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
        double scale = client.getWindow().getScaleFactor();
        int windowWidth = client.getWindow().getWidth();
        int windowHeight = client.getWindow().getHeight();

        double centerX = (windowWidth / scale / 2.0) - 8.0;
        double centerY = (windowHeight / scale / 2.0) - 8.0;

        int x = (int) Math.round(centerX + offsetX);
        int y = (int) Math.round(centerY + offsetY);

        return new Point(x, y);
    }

    public void setOffset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setSize(float size) {
        this.size = MathHelper.clamp(size, 0.1f, 10.0f);
    }

    public float getSize() {
        return this.size;
    }

    public PixelGridAnimation copy() {
        List<PixelGrid> copiedFrames = frames.stream().map(PixelGrid::copy).toList();
        PixelGridAnimation copy = new PixelGridAnimation(copiedFrames);
        copy.currentFrame = this.currentFrame;
        copy.animationSpeed = this.animationSpeed;
        copy.offsetX = this.offsetX;
        copy.offsetY = this.offsetY;
        copy.size = this.size;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PixelGridAnimation other)) return false;
        if (this.animationSpeed != other.animationSpeed) return false;
        if (this.offsetX != other.offsetX || this.offsetY != other.offsetY) return false;
        if (this.size != other.size) return false;
        if (this.frames.size() != other.frames.size()) return false;

        for (int i = 0; i < frames.size(); i++) {
            if (!this.frames.get(i).equals(other.frames.get(i))) return false; //deepcopy?
        }

        return true;
    }
}
