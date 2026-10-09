package main.walksy.lib.core.gui.widget.options.category;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.gui.widget.ScrollGlide;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.util.Mth;

public class CategorySidebarWidget extends WLWidget {
    private static final float PANEL_GAP = 10f;
    private static final float CORNER_RADIUS = 12f;
    private static final float PRESENCE_SPEED = 11f;
    private static final float REST_SCALE = 0.90f;
    private static final float SLIDE_END = 0.62f;
    private static final float SCROLL_ROWS = 2f;
    private static final float SCROLL_GLIDE_SPEED = 14f;
    private static float presence;
    private static boolean wanted;
    private final List<Category> categories;
    private final CategoryListView list;
    private final float uiScale;
    private final float clipLeft;
    private final float centerY;
    private final int maxHeight;
    private float scrollY;
    private float scrollTarget;
    private final ScrollGlide glide = new ScrollGlide();

    public static void setWanted(final boolean value) {
        wanted = value;
    }

    public static void snapToWanted() {
        presence = wanted ? 1f : 0f;
    }

    public static void tickPresence(final float dt) {
        if (!Float.isFinite(dt) || dt <= 0f) {
            return;
        }
        final float target = wanted ? 1f : 0f;
        presence = UiStyle.minecraft() ? target : presence + (target - presence) * (1f - (float) Math.exp(-PRESENCE_SPEED * dt));
        if (presence > 0.999f) {
            presence = 1f;
        } else if (presence < 0.001f) {
            presence = 0f;
        }
    }

    public static boolean isGone() {
        return presence <= 0.001f;
    }

    public boolean isLeaving() {
        return !wanted;
    }

    public CategorySidebarWidget(final List<Category> categories, final Consumer<Category> onSelect, final Supplier<Category> selectedSupplier, final float panelRight, final float centerY, final int maxHeight) {
        super(0, 0, 1, 1);
        this.categories = categories;
        this.centerY = centerY;
        this.maxHeight = maxHeight;
        this.uiScale = WLScreen.uiScaleFactor();
        this.list = new CategoryListView(categories, selectedSupplier, onSelect, this.uiScale, true);
        this.setWidth(Math.max(1, Math.round(this.list.preferredWidth())));
        this.setHeight(Math.max(1, Math.min(maxHeight, Math.round(this.list.preferredHeight()))));
        this.clipLeft = panelRight;
        this.setX(Math.round(panelRight + PANEL_GAP * this.uiScale));
        this.setY(Math.round(centerY - this.getHeight() / 2f));
    }

    public boolean matches(final List<Category> categories, final float panelRight, final float centerY, final int maxHeight) {
        return this.categories.equals(categories) && this.getX() == Math.round(panelRight + PANEL_GAP * this.uiScale) && this.centerY == centerY && this.maxHeight == maxHeight && this.uiScale == WLScreen.uiScaleFactor();
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        final float p = presence;
        if (alpha <= 0.001f || p <= 0.001f) {
            return;
        }
        float dt = WLRenderSystem.frameDeltaSeconds();
        if (!Float.isFinite(dt) || dt < 0f) {
            dt = 0f;
        }
        final float slideT = smooth(Mth.clamp(p / SLIDE_END, 0f, 1f));
        final float standT = smooth(Mth.clamp((p - SLIDE_END) / (1f - SLIDE_END), 0f, 1f));
        final float slide = (this.getWidth() + PANEL_GAP * this.uiScale) * (1f - slideT);
        final float scale = REST_SCALE + (1f - REST_SCALE) * standT;
        final float cx = this.getX() + this.getWidth() / 2f;
        final float cy = this.getY() + this.getHeight() / 2f;
        final float fade = alpha * slideT;
        final int clip = Math.round(this.clipLeft);
        graphics.enableScissor(clip, 0, clip + 10_000, 10_000);
        final var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(-slide, 0f);
        pose.translate(cx, cy);
        pose.scale(scale, scale);
        pose.translate(-cx, -cy);
        try {
            graphics.glass()
                .dimensions(this.getX(), this.getY(), this.getWidth(), this.getHeight())
                .cornerRadius(CORNER_RADIUS * this.uiScale)
                .style(WalksyLibPanelScreen.panelStyle().scaled(this.uiScale).layer(WalksyLibPanelScreen.SIDEBAR_LAYER).opacity(fade))
                .extract();
            graphics.enableScissor(Math.round(this.getX()), Math.round(this.getY()), Math.round(this.getX() + this.getWidth()), Math.round(this.getY() + this.getHeight()));
            try {
                this.scrollTarget = Mth.clamp(this.scrollTarget, 0f, this.list.maxScroll(this.getHeight()));
                this.scrollY = Mth.clamp(this.glide.step(this.scrollY, this.scrollTarget, SCROLL_GLIDE_SPEED, dt), 0f, this.list.maxScroll(this.getHeight()));
                final TextQueueMark textMark = WLRenderSystem.textMark();
                WLRenderSystem.contentStratum().begin();
                try {
                    this.list.extract(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), mouseX, mouseY, dt, fade, ScrollGlide.shown(this.scrollY));
                    this.list.extractScrollIndicator(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.scrollY, fade);
                } finally {
                    WLRenderSystem.contentStratum().end();
                    WLRenderSystem.markContentText(textMark);
                }
            } finally {
                graphics.disableScissor();
            }
        } finally {
            pose.popMatrix();
            graphics.disableScissor();
        }
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        final float max = this.list.maxScroll(this.getHeight());
        if (max <= 0f || presence < 0.999f || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scrollTarget = Mth.clamp(this.scrollTarget - (float) scrollY * SCROLL_ROWS * this.list.rowHeight(), 0f, max);
        return true;
    }

    private static float smooth(final float t) {
        return t * t * (3f - 2f * t);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.active || presence < 0.999f || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }
        return this.list.click(event.y(), this.getY(), this.getHeight(), this.scrollY);
    }
}
