package main.walksy.lib.core.gui.widget;

import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.platform.WLInput;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

public class ScrollPanel extends WLWidget {
    private static final float WHEEL_STEP = 30f;
    private static final float SCROLL_SPEED = 14f;
    private static final float BOTTOM_SLOP = 2f;
    private static final float TOP_FADE_CURVE = 0.55f;
    private static final int EDGE_LAYER = 16;
    private static final float SCROLLBAR_WIDTH = 2.5f;
    private static final float THUMB_HIT_SLOP = 3f;
    private static final int SCROLLBAR_GUTTER = 6;
    private static final int MIN_THUMB_HEIGHT = 16;
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x5C401L);
    private final long edgeFadeKey = FADE_KEYS.incrementAndGet();
    private int topEdgeY = Integer.MIN_VALUE;
    private final List<AbstractWidget> children = new ArrayList<>();
    private final List<Object> keys = new ArrayList<>();
    private final List<Integer> logicalY = new ArrayList<>();
    private final List<Integer> gaps = new ArrayList<>();
    private final Map<Object, AbstractWidget> recycled = new HashMap<>();
    private AbstractWidget recycledFocus;
    private int contentHeight;
    private float scrollY;
    private float scrollTarget;
    private boolean carryPending;
    private float carriedScrollY;
    private float carriedScrollTarget;
    private final ScrollGlide glide = new ScrollGlide();
    private boolean draggingThumb;
    private float thumbDragGrabOffset;
    private AbstractWidget focusedChild;
    private Runnable deferred;
    private BooleanSupplier staleWhen;
    private Runnable staleRebuild;
    private int contentInsetTop;
    private float scrollbarEndInset;
    private float scrollbarRightShift;

    @FunctionalInterface
    public interface ChildFactory<T extends AbstractWidget> {
        T create(int x, int y, int width);
    }

    @FunctionalInterface
    public interface Revalidating {
        void revalidateSize();
    }

    public interface ClickInterceptor {
        boolean interceptClick(MouseButtonEvent event);
    }

    public interface ExtendedPaint {

        float paintBottom();

        void setHeaderAlpha(float alpha);

        void setDissolveBand(float top, float height);
    }

    private static float s() {
        return WLScreen.uiScaleFactor();
    }

    public ScrollPanel(final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
    }

    public void moveTo(final int x, final int y) {
        final int dx = x - this.getX();
        this.setX(x);
        this.setY(y);
        if (dx != 0) {
            for (final AbstractWidget child : this.children) {
                child.setX(child.getX() + dx);
            }
        }
    }

    public void setContentInsetTop(final int px) {
        this.contentInsetTop = Math.max(0, px);
        this.relayout();
    }

    public void setScrollbarInsets(final float endInset, final float rightShift) {
        this.scrollbarEndInset = Math.max(0f, endInset) * s();
        this.scrollbarRightShift = rightShift * s();
    }

    private float barWidth() {
        return SCROLLBAR_WIDTH * s();
    }

    private float trackX1() {
        return this.getX() + this.getWidth() + this.scrollbarRightShift;
    }

    private float trackX0() {
        return this.trackX1() - this.barWidth();
    }

    private float trackTopY() {
        return this.getY() + this.scrollbarEndInset;
    }

    private float trackLen() {
        return Math.max(1f, this.getHeight() - this.scrollbarEndInset * 2f);
    }

    public <T extends AbstractWidget> T addChild(final int gapBefore, final ChildFactory<T> factory) {
        return this.addChild(gapBefore, null, factory);
    }

    public <T extends AbstractWidget> T addChild(final int gapBefore, final Object key, final ChildFactory<T> factory) {
        final int gap = this.children.isEmpty() ? 0 : gapBefore;
        final int y = (this.children.isEmpty() ? this.contentInsetTop : this.contentHeight) + gap;
        final T child = this.claim(key, y, factory);
        this.children.add(child);
        this.keys.add(key);
        this.logicalY.add(y);
        this.gaps.add(gap);
        this.contentHeight = y + child.getHeight();
        return child;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractWidget> T claim(final Object key, final int y, final ChildFactory<T> factory) {
        final AbstractWidget kept = key == null ? null : this.recycled.remove(key);
        if (kept != null && kept.getWidth() == this.contentWidth()) {
            kept.setX(this.getX());
            kept.setY(this.getY() + y);
            if (kept == this.recycledFocus) {
                this.focusedChild = kept;
            }
            if (kept instanceof final Revalidating revalidating) {
                revalidating.revalidateSize();
            }
            return (T) kept;
        }
        if (kept != null) {
            kept.setFocused(false);
        }
        return factory.create(this.getX(), this.getY() + y, this.contentWidth());
    }

    public int contentWidth() {
        return Math.max(1, Math.round(this.getWidth() - (SCROLLBAR_WIDTH + SCROLLBAR_GUTTER) * s()));
    }

    public int contentHeight() {
        return this.contentHeight;
    }

    public void relayout() {
        final boolean atBottom = this.atBottom();
        final float previousMax = this.maxScroll();
        int y = this.contentInsetTop;
        for (int i = 0; i < this.children.size(); i++) {
            final int gap = this.gaps.get(i);
            y += gap;
            this.logicalY.set(i, y);
            y += this.children.get(i).getHeight();
        }
        this.contentHeight = y;
        this.scrollY = this.clampScroll(this.scrollY);
        this.followGrowth(atBottom, previousMax);
    }

    public void carryScroll(final ScrollPanel previous) {
        if (previous == null) {
            return;
        }
        this.carryPending = true;
        this.carriedScrollY = previous.scrollY;
        this.carriedScrollTarget = previous.scrollTarget;
    }

    public void defer(final Runnable action) {
        this.deferred = action;
    }

    public void rebuildWhenStale(final BooleanSupplier stale, final Runnable rebuild) {
        this.staleWhen = stale;
        this.staleRebuild = rebuild;
    }

    private void revalidate() {
        final BooleanSupplier stale = this.staleWhen;
        final Runnable rebuild = this.staleRebuild;
        if (stale == null || rebuild == null || !stale.getAsBoolean()) {
            return;
        }
        this.staleWhen = null;
        this.staleRebuild = null;
        final float keepScrollY = this.scrollY;
        final float keepScrollTarget = this.scrollTarget;
        final boolean atBottom = this.atBottom();
        final float previousMax = this.maxScroll();
        rebuild.run();
        this.flushDeferred();
        this.scrollY = this.clampScroll(keepScrollY);
        this.scrollTarget = this.clampScroll(keepScrollTarget);
        this.followGrowth(atBottom, previousMax);
    }

    public void clear() {
        this.dropRecycled();
        for (int i = 0; i < this.children.size(); i++) {
            final Object key = this.keys.get(i);
            if (key != null) {
                this.recycled.put(key, this.children.get(i));
            }
        }
        this.recycledFocus = this.focusedChild;
        this.children.clear();
        this.keys.clear();
        this.logicalY.clear();
        this.gaps.clear();
        this.contentHeight = 0;
        this.scrollY = 0f;
        this.scrollTarget = 0f;
        this.focusedChild = null;
        this.draggingThumb = false;
    }

    private void dropRecycled() {
        for (final AbstractWidget widget : this.recycled.values()) {
            widget.setFocused(false);
        }
        this.recycled.clear();
        this.recycledFocus = null;
    }

    private void flushDeferred() {
        final Runnable action = this.deferred;
        if (action == null) {
            return;
        }
        this.deferred = null;
        action.run();
    }

    public List<AbstractWidget> children() {
        return this.children;
    }

    private void extractTopEdge(final WLGraphics2D graphics, final float alpha) {
        if (alpha <= 0.001f || this.edgeBand() <= 0f) {
            return;
        }
        final float full = this.edgeBand();
        if (full >= 1f) {
            graphics.glass()
                .dimensions(this.getX(), this.edgeTop(), this.getWidth(), full)
                .cornerRadius(0f)
                .style(new GlassStyle()
                    .fadeKey(this.edgeFadeKey)
                    .layer(EDGE_LAYER)
                    .blurRadius(LiquidGlassConstants.BLUR_POPOVER)
                    .blurFalloff(true)
                    .tint(0xFFFFFF, 0f)
                    .refractionStrength(0f)
                    .refractionThickness(0f)
                    .refractionFactor(1f)
                    .refractionDispersion(0f)
                    .fresnelRange(0f)
                    .fresnelHardness(0f)
                    .fresnelFactor(0f)
                    .glareRange(0f)
                    .glareHardness(0f)
                    .edgeDarken(0f)
                    .brightEdge(0f)
                    .rimLight(Theme.get().rimLightColor(), 0f)
                    .shadow(0f, 0f, 0f, 0f)
                    .shadowColor(Theme.get().shadowColor(), 0f)
                    .scaled(s())
                    .opacity(alpha))
                .extract();
        }
    }

    public void setTopEdge(final int absoluteY) {
        this.topEdgeY = absoluteY;
    }

    private float edgeBand() {
        return Math.max(0f, this.getY() - this.edgeTop());
    }

    private int edgeTop() {
        return this.topEdgeY == Integer.MIN_VALUE ? this.getY() : Math.min(this.topEdgeY, this.getY());
    }

    private float maxScroll() {
        return Math.max(0f, this.contentHeight - this.getHeight());
    }

    private boolean atBottom() {
        final float max = this.maxScroll();
        return max > 0f && this.scrollTarget >= max - BOTTOM_SLOP * s();
    }

    private void followGrowth(final boolean atBottom, final float previousMax) {
        if (atBottom && this.maxScroll() > previousMax) {
            this.scrollTarget = this.maxScroll();
        }
    }

    private float clampScroll(final float v) {
        return Math.max(0f, Math.min(v, this.maxScroll()));
    }

    private void syncChildPositions() {
        final int offset = Math.round(this.scrollY);
        for (int i = 0; i < this.children.size(); i++) {
            this.children.get(i).setY(this.getY() + this.logicalY.get(i) - offset);
        }
    }

    private void setFocusedChild(final AbstractWidget child) {
        if (this.focusedChild == child) {
            return;
        }
        if (this.focusedChild != null) {
            this.focusedChild.setFocused(false);
        }
        this.focusedChild = child;
        if (child != null) {
            child.setFocused(true);
        }
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        this.flushDeferred();
        this.revalidate();
        if (this.carryPending) {
            this.carryPending = false;
            this.scrollY = Math.max(0f, this.carriedScrollY);
            this.scrollTarget = this.carriedScrollTarget;
        }
        this.scrollTarget = this.clampScroll(this.scrollTarget);
        this.scrollY = this.draggingThumb ? this.clampScroll(this.scrollY) : Math.max(0f, this.scrollY);
        if (!this.draggingThumb && this.scrollY != this.scrollTarget) {
            this.scrollY = this.glide.step(this.scrollY, this.scrollTarget, SCROLL_SPEED, WLRenderSystem.frameDeltaSeconds());
        }
        this.syncChildPositions();
        final int x0 = this.getX();
        final int x1 = this.getX() + this.getWidth();
        final int y1 = this.getY() + this.getHeight();
        final int y0 = this.edgeTop();
        graphics.enableScissor(x0, y0, x1, y1);
        final var pose = graphics.pose();
        pose.pushMatrix();
        final float poseShift = DevicePixels.snapY(Math.round(this.scrollY) - this.scrollY, pose);
        pose.translate(0f, poseShift);
        final float alpha = this.getAlpha();
        final TextQueueMark textMark = WLRenderSystem.textMark();
        WLRenderSystem.contentStratum().begin();
        try {
            final float fadeBand = this.edgeBand();
            for (final AbstractWidget child : this.children) {
                final ExtendedPaint tall = child instanceof ExtendedPaint ep ? ep : null;
                final float paintBottom = tall != null ? tall.paintBottom() : child.getY() + child.getHeight();
                if (paintBottom < y0 || child.getY() > y1) {
                    continue;
                }
                final float ownFade = dissolve((child.getY() + child.getHeight() / 2f) - y0, fadeBand);
                float fade = ownFade;
                if (tall != null) {
                    fade = dissolve(paintBottom - y0, fadeBand);
                    tall.setHeaderAlpha(alpha * ownFade);
                    tall.setDissolveBand(y0 - poseShift, fadeBand);
                }
                if (fade <= 0f) {
                    continue;
                }
                child.setAlpha(alpha * fade);
                graphics.widget(child, mouseX, mouseY, a);
            }
        } finally {
            WLRenderSystem.contentStratum().end();
            WLRenderSystem.markContentText(textMark);
            pose.popMatrix();
        }
        graphics.disableScissor();
        this.extractTopEdge(graphics, alpha);
        WLRenderSystem.contentStratum().begin();
        try {
            this.extractScrollbar(graphics, mouseX, mouseY);
        } finally {
            WLRenderSystem.contentStratum().end();
        }
    }

    private static float dissolve(final float leaving, final float fadeBand) {
        final float linear = fadeBand <= 0f ? 1f : Math.max(0f, Math.min(1f, leaving / fadeBand));
        return linear >= 1f ? 1f : (float) Math.pow(linear, TOP_FADE_CURVE);
    }

    private void extractScrollbar(final WLGraphics2D graphics, final int mouseX, final int mouseY) {
        final float max = this.maxScroll();
        if (max <= 0.5f) {
            return;
        }
        final float trackX0 = this.trackX0();
        final float trackX1 = this.trackX1();
        final float trackY0 = this.trackTopY();
        final float trackY1 = trackY0 + this.trackLen();
        final float radius = this.barWidth() / 2f;
        final int groove = ColorUtils.withAlpha(Theme.get().separator(), 0.45f);
        graphics.fillRoundedGradient(trackX0, trackY0, trackX1, trackY1, groove, groove, groove, groove, radius);
        final float thumbH = this.thumbHeight();
        final float thumbY = this.thumbTop();
        WLRenderSystem.cursors().requestIf(this.draggingThumb || this.isOverThumb(mouseX, mouseY), Cursors.Cursor.RESIZE_NS);
        final int thumb = Theme.get().secondaryLabel();
        graphics.fillRoundedGradient(trackX0, thumbY, trackX1, thumbY + thumbH, thumb, thumb, thumb, thumb, radius);
    }

    private float thumbHeight() {
        final float total = this.contentHeight + this.getHeight();
        final float ratio = total <= 0f ? 1f : this.getHeight() / total;
        return Math.max(MIN_THUMB_HEIGHT * s(), this.trackLen() * ratio);
    }

    private float thumbTop() {
        final float max = this.maxScroll();
        final float travel = Math.max(0f, this.trackLen() - this.thumbHeight());
        final float t = max <= 0f ? 0f : Math.min(1f, this.scrollY / max);
        return this.trackTopY() + travel * t;
    }

    private boolean isOverThumb(final double mouseX, final double mouseY) {
        if (this.maxScroll() <= 0.5f) {
            return false;
        }
        final float thumbY0 = this.thumbTop();
        final float thumbY1 = thumbY0 + this.thumbHeight();
        final float slop = THUMB_HIT_SLOP * s();
        return mouseX >= this.trackX0() - slop && mouseX < this.trackX1() + slop && mouseY >= thumbY0 && mouseY < thumbY1;
    }

    @Override
    public boolean isMouseOver(final double mouseX, final double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || this.active && this.visible && this.isOverThumb(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        this.syncChildPositions();
        if (this.isOverThumb(event.x(), event.y())) {
            this.draggingThumb = true;
            this.thumbDragGrabOffset = (float) event.y() - this.thumbTop();
            return true;
        }
        for (final AbstractWidget child : this.children) {
            if (child.isMouseOver(event.x(), event.y())) {
                this.setFocusedChild(child);
                if (child instanceof final ClickInterceptor interceptor && interceptor.interceptClick(event)) {
                    return true;
                }
                return WLInput.mouseClicked(child, event, doubleClick);
            }
        }
        this.setFocusedChild(null);
        return false;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.draggingThumb) {
            final float travel = this.trackLen() - this.thumbHeight();
            final float t = travel <= 0f ? 0f : ((float) event.y() - this.thumbDragGrabOffset - this.trackTopY()) / travel;
            this.scrollY = this.clampScroll(t * this.maxScroll());
            this.scrollTarget = this.scrollY;
            return true;
        }
        if (this.focusedChild != null) {
            return WLInput.mouseDragged(this.focusedChild, event, dragX, dragY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        this.draggingThumb = false;
        if (this.focusedChild != null) {
            return WLInput.mouseReleased(this.focusedChild, event);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scrollTarget = this.clampScroll(this.scrollTarget - (float) scrollY * WHEEL_STEP * s());
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.focusedChild != null && WLInput.keyPressed(this.focusedChild, event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.focusedChild != null && WLInput.charTyped(this.focusedChild, event);
    }
}
