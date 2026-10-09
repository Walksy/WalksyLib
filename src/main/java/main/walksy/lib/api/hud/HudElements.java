package main.walksy.lib.api.hud;

import main.walksy.lib.api.config.value.PixelGrid;
import main.walksy.lib.api.config.value.PixelGridAnimation;
import main.walksy.lib.core.render.WLGraphics2D;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class HudElements {
    private HudElements() {
    }

    public static @Nullable HudElement forValue(final Class<?> type, final Supplier<Object> getter) {
        if (type == PixelGridAnimation.class) {
            return pixelGrid(() -> (PixelGridAnimation) getter.get());
        }
        return null;
    }

    public static HudElement when(final BooleanSupplier condition, final HudElement element) {
        return new HudElement() {
            @Override
            public void render(final WLGraphics2D graphics, final float x, final float y, final float scale, final boolean editing) {
                if (editing || condition.getAsBoolean()) {
                    element.render(graphics, x, y, scale, editing);
                }
            }

            @Override
            public float width(final float scale) {
                return element.width(scale);
            }

            @Override
            public float scaleStep() {
                return element.scaleStep();
            }

            @Override
            public float height(final float scale) {
                return element.height(scale);
            }

            @Override
            public boolean visible() {
                return element.visible() && condition.getAsBoolean();
            }

            @Override
            public float defaultOffsetX() {
                return element.defaultOffsetX();
            }

            @Override
            public float defaultOffsetY() {
                return element.defaultOffsetY();
            }
        };
    }

    public static HudElement box(final DoubleSupplier width, final DoubleSupplier height) {
        return new HudElement() {
            @Override
            public void render(final WLGraphics2D graphics, final float x, final float y, final float scale, final boolean editing) {
            }

            @Override
            public float width(final float scale) {
                return (float) width.getAsDouble() * scale;
            }

            @Override
            public float height(final float scale) {
                return (float) height.getAsDouble() * scale;
            }
        };
    }

    public static HudElement box(final float width, final float height) {
        return box(() -> width, () -> height);
    }

    public static HudElement pixelGrid(final Supplier<PixelGridAnimation> animation) {
        return pixelGrid(animation, () -> true);
    }

    public static HudElement pixelGrid(final Supplier<PixelGridAnimation> animation, final BooleanSupplier blend) {
        return new HudElement() {
            @Override
            public void render(final WLGraphics2D graphics, final float x, final float y, final float scale, final boolean editing) {
                final PixelGridAnimation value = animation.get();
                final PixelGrid frame = value == null ? null : value.getCurrentFrame();
                if (frame == null) {
                    return;
                }
                frame.render(graphics, x, y, cellPixels(value, scale), blend.getAsBoolean());
            }

            @Override
            public float defaultOffsetX() {
                final PixelGridAnimation value = animation.get();
                return value == null ? 0f : (float) value.getOffsetX();
            }

            @Override
            public float defaultOffsetY() {
                final PixelGridAnimation value = animation.get();
                return value == null ? 0f : (float) value.getOffsetY();
            }

            @Override
            public float scaleStep() {
                final PixelGridAnimation value = animation.get();
                return value == null || value.getSize() <= 0f ? 1f : 1f / value.getSize();
            }

            @Override
            public float width(final float scale) {
                final PixelGridAnimation value = animation.get();
                final var frame = value == null ? null : value.getCurrentFrame();
                return frame == null ? 0f : frame.getWidth() * cellPixels(value, scale);
            }

            @Override
            public float height(final float scale) {
                final PixelGridAnimation value = animation.get();
                final var frame = value == null ? null : value.getCurrentFrame();
                return frame == null ? 0f : frame.getHeight() * cellPixels(value, scale);
            }
        };
    }

    private static int cellPixels(final PixelGridAnimation value, final float scale) {
        return Math.max(1, Math.round(value.getSize() * scale));
    }
}
