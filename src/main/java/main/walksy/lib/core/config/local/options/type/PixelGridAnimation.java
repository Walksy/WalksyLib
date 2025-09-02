package main.walksy.lib.core.config.local.options.type;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PixelGridAnimation {
    private List<PixelGrid> frames = new ArrayList<>();
    public int currentFrame = 0;

    public PixelGridAnimation(PixelGrid... grids)
    {
        frames.addAll(Arrays.asList(grids));
    }

    public PixelGridAnimation(List<PixelGrid> grids)
    {
        frames.addAll(grids);
    }

    //this is stupid
    public PixelGridAnimation(PixelGridAnimation original, PixelGrid addition, int index)
    {
        List<PixelGrid> newFrames = new ArrayList<>();
        for (int x = 0; x < original.getFrames().size(); x++)
        {
            if (x == index - 1) {
                newFrames.add(addition);
            } else {
                newFrames.add(original.getFrames().get(x));
            }
        }
        frames.addAll(newFrames);
    }

    public static PixelGridAnimation replace(PixelGridAnimation original, PixelGrid gridReplace, int index) {
        PixelGridAnimation result = new PixelGridAnimation(original, gridReplace, index);
        result.setCurrentFrame(original.currentFrame);
        return result;
    }


    public PixelGrid getCurrentFrame() {
        if (frames.isEmpty()) return null;
        return frames.get(currentFrame);
    }

    public List<PixelGrid> getFrames()
    {
        return this.frames;
    }

    public PixelGrid getFrame(int i)
    {
        return frames.get(i - 1);
    }

    public void setFrame(int i, PixelGrid grid)
    {
        frames.set(i - 1, grid);
    }

    public PixelGridAnimation copy() {
        List<PixelGrid> newFrames = frames.stream().map(PixelGrid::copy).toList();
        PixelGridAnimation copy = new PixelGridAnimation(newFrames);
        copy.currentFrame = this.currentFrame;
        return copy;
    }

    public void setCurrentFrame(int index) {
        this.currentFrame = MathHelper.clamp(index, 0, frames.size() - 1);
    }

    public void addFrame(PixelGrid grid) {
        frames.add(grid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PixelGridAnimation other)) return false;

        if (this.currentFrame != other.currentFrame) return false;
        if (this.frames.size() != other.frames.size()) return false;

        for (int i = 0; i < frames.size(); i++) {
            PixelGrid thisGrid = this.frames.get(i);
            PixelGrid otherGrid = other.frames.get(i);
            if (!thisGrid.equals(otherGrid)) {
                return false;
            }
        }

        return true;
    }
}

