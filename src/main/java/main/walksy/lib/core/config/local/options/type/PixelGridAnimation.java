package main.walksy.lib.core.config.local.options.type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.List;

public class PixelGridAnimation {
    private final List<PixelGrid> frames = new ArrayList<>();
    private int currentFrame = 0;
    private int tickCounter = 0;
    private int animationSpeed = 10;

    private double offsetX = 0.0;
    private double offsetY = 0.0;

    private float size = 1.0f;
    private boolean hasPlayedOnce = false;

    public PixelGridAnimation(PixelGrid... grids) {
        List<PixelGrid> grids1 = new ArrayList<>(List.of(grids));
        if (grids1.isEmpty()) {
            grids1.add(PixelGrid.create().build());
        }
        frames.addAll(grids1);
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
        hasPlayedOnce = false;
    }

    public void render(DrawContext context, boolean blend) {
        Vec2f pos = getAbsolutePosition();
        this.render(context, pos.x, pos.y, blend);
    }

    public void render(DrawContext context, float x, float y, boolean blend) {
        PixelGrid frame = this.getCurrentFrame();
        if (frame != null) {
            context.getMatrices().push();
            context.getMatrices().scale(size, size, 1.0f);
            frame.render(context, x / size, y / size, blend);
            context.getMatrices().pop();
        }
    }

    public void tick() {
        if (frames.isEmpty() || animationSpeed <= 0) return;
        tickCounter++;
        int frameDelay = Math.max(1, 21 - animationSpeed);
        if (tickCounter >= frameDelay) {
            tickCounter = 0;
            int prevFrame = currentFrame;
            currentFrame = (currentFrame + 1) % frames.size();
            if (currentFrame == 0 && prevFrame == frames.size() - 1) {
                hasPlayedOnce = true;
            }
        }
    }

    public boolean hasPlayedOnce() {
        return hasPlayedOnce;
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
        int adjusted = index - 1;
        if (adjusted < 0 || adjusted >= frames.size()) return null;
        return frames.get(adjusted);
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

    public Vec2f getAbsolutePosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return new Vec2f(0f, 0f);

        int windowWidth = client.getWindow().getScaledWidth();
        int windowHeight = client.getWindow().getScaledHeight();

        PixelGrid frame = getCurrentFrame();
        int frameW = frame == null ? 0 : frame.getWidth();
        int frameH = frame == null ? 0 : frame.getHeight();

        int renderedW = Math.round(frameW * size);
        int renderedH = Math.round(frameH * size);

        int baseX = (windowWidth - renderedW) / 2;
        int baseY = (windowHeight - renderedH) / 2;

        double rawX = baseX + offsetX;
        double rawY = baseY + offsetY;

        float x = (float) (Math.round(rawX * 2.0) / 2.0);
        float y = (float) (Math.round(rawY * 2.0) / 2.0);

        return new Vec2f(x, y);
    }

    public PixelGridAnimation offset(double offsetX, double offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        return this;
    }

    public void setOffset(double offsetX, double offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public void addOffset(double dx, double dy) {
        this.offsetX += dx;
        this.offsetY += dy;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public PixelGridAnimation animationSpeed(int speed) {
        this.animationSpeed = speed;
        return this;
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
            if (!this.frames.get(i).equals(other.frames.get(i))) return false;
        }
        return true;
    }
}
