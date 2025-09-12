package main.walksy.lib.core.config.local.options.type;

import main.walksy.lib.core.WalksyLib;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class PixelGridAnimation {
    private final List<PixelGrid> frames = new ArrayList<>();
    private Supplier<Point> position;
    private int currentFrame = 0;
    private int tickCounter = 0;
    private int animationSpeed = 10;

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
        if (this.position == null || (this.position.get().x == -1) && (this.position.get().y == -1))
        {
            //TODO LOG
            return;
        }
        WalksyLib.getInstance().get2DRenderer().renderGridTexture(context, this.getCurrentFrame(), this.position.get().x, this.position.get().y, 1, 0, 1);
    }

    public void render(DrawContext context, int x, int y) {
        WalksyLib.getInstance().get2DRenderer().renderGridTexture(context, this.getCurrentFrame(), x, y, 1, 0, 1);
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

    public Point getPosition()
    {
        return this.position.get();
    }

    public void setPosition(Supplier<Point> position)
    {
        this.position = position;
    }

    public PixelGridAnimation copy() {
        List<PixelGrid> copiedFrames = frames.stream().map(PixelGrid::copy).toList();
        PixelGridAnimation copy = new PixelGridAnimation(copiedFrames);
        copy.currentFrame = this.currentFrame;
        copy.animationSpeed = this.animationSpeed;
        copy.setPosition(this.position);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PixelGridAnimation other)) return false;
        if (this.animationSpeed != other.animationSpeed) return false;
        if (!this.position.equals(other.position)) return false;
        if (this.frames.size() != other.frames.size()) return false;

        for (int i = 0; i < frames.size(); i++) {
            if (!this.frames.get(i).equals(other.frames.get(i))) return false;
        }

        return true;
    }
}
