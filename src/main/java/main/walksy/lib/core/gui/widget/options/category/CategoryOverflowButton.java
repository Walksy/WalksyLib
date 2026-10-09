package main.walksy.lib.core.gui.widget.options.category;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.gui.widget.ScrollGlide;
import net.minecraft.util.Mth;

public class CategoryOverflowButton extends WLWidget {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x5C0FF0L);
    private static final float CHEVRON_RATIO = 0.42f;
    private static final float HOVER_SPEED = 14f;
    private static final float SCROLL_ROWS = 2f;
    private static final float SCROLL_GLIDE_SPEED = 18f;
    private static final float PANEL_GAP = 10f;
    private final List<Category> categories;
    private final Consumer<Category> onSelect;
    private final Supplier<Category> selectedSupplier;
    private final RowContext.OpenAt openAt;
    private final Runnable closePopover;
    private final float panelRight;
    private final int maxListHeight;
    private final float panelCenterY;
    private final float uiScale;
    private final GlassStyle style;
    private float hoverAnim;
    private boolean popoverActive;

    public CategoryOverflowButton(final List<Category> categories, final Consumer<Category> onSelect, final Supplier<Category> selectedSupplier, final RowContext.OpenAt openAt, final Runnable closePopover, final float panelRight, final int maxListHeight, final float panelCenterY, final int x, final int y, final int size) {
        super(x, y, size, size);
        this.categories = categories;
        this.onSelect = onSelect;
        this.selectedSupplier = selectedSupplier;
        this.openAt = openAt;
        this.closePopover = closePopover;
        this.panelRight = panelRight;
        this.maxListHeight = Math.max(1, maxListHeight);
        this.panelCenterY = panelCenterY;
        this.uiScale = WLScreen.uiScaleFactor();
        this.style = WalksyLibPanelScreen.floatingChromeStyle()
            .layer(CategoryTabBar.BAR_LAYER)
            .tint(WalksyLibPanelScreen.fieldTintColor(), WalksyLibPanelScreen.fieldTintAlpha())
            .fadeKey(FADE_KEYS.incrementAndGet());
    }

    public boolean matches(final List<Category> categories, final int maxListHeight, final float panelCenterY, final int x, final int y, final int size) {
        return this.categories.equals(categories) && this.maxListHeight == Math.max(1, maxListHeight) && this.panelCenterY == panelCenterY && this.getX() == x && this.getY() == y && this.getWidth() == size && this.uiScale == WLScreen.uiScaleFactor();
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f);
        if (alpha <= 0.001f || this.popoverActive) {
            if (this.popoverActive) {
                this.hoverAnim = 0f;
            }
            return;
        }
        float dt = WLRenderSystem.frameDeltaSeconds();
        if (!Float.isFinite(dt) || dt < 0f) {
            dt = 0f;
        }
        final boolean hovered = this.isMouseOver(mouseX, mouseY) && this.active;
        WLRenderSystem.cursors().requestControl(this.isMouseOver(mouseX, mouseY), this.active);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        graphics.glass()
            .dimensions(this.getX(), this.getY(), this.getWidth(), this.getHeight())
            .cornerRadius(this.getWidth() / 2f)
            .style(this.style.scaled(this.uiScale).opacity(alpha))
            .hover(this.hoverAnim)
            .extract();
        final float iconSize = this.getWidth() * CHEVRON_RATIO;
        Icons.CHEVRON.extract(graphics, this.getX() + (this.getWidth() - iconSize) / 2f, this.getY() + (this.getHeight() - iconSize) / 2f, iconSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.active || this.popoverActive || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }
        if (this.openAt == null) {
            return false;
        }
        this.popoverActive = true;
        final LiquidRect anchor = new LiquidRect(this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.getWidth() / 2f);
        final CategoryListPopover content = new CategoryListPopover();
        final float y = this.panelCenterY - content.preferredHeight() / 2f;
        this.openAt.accept(anchor, this.panelRight + PANEL_GAP * this.uiScale, y, content);
        return true;
    }

    private final class CategoryListPopover implements PopoverHost.PopoverContent {
        private final CategoryListView list = new CategoryListView(CategoryOverflowButton.this.categories, CategoryOverflowButton.this.selectedSupplier, CategoryOverflowButton.this.onSelect, CategoryOverflowButton.this.uiScale, true);
        private float scrollY;
        private float scrollTarget;
        private final ScrollGlide glide = new ScrollGlide();

        @Override
        public GlassStyle originStyle() {
            return WalksyLibPanelScreen.panelStyle().scaled(CategoryOverflowButton.this.uiScale).layer(PopoverHost.PANEL_LAYER);
        }

        @Override
        public boolean keepsOriginStyle() {
            return true;
        }

        @Override
        public boolean bridged() {
            return false;
        }

        @Override
        public void onClosed() {
            CategoryOverflowButton.this.popoverActive = false;
        }

        @Override
        public int preferredWidth() {
            return Math.round(this.list.preferredWidth());
        }

        @Override
        public int preferredHeight() {
            return Math.min(CategoryOverflowButton.this.maxListHeight, Math.round(this.list.preferredHeight()));
        }

        @Override
        public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
            final float dtSeconds = delta / 20f;
            final float max = this.list.maxScroll(panelH);
            this.scrollTarget = Mth.clamp(this.scrollTarget, 0f, max);
            this.scrollY = Mth.clamp(this.glide.step(this.scrollY, this.scrollTarget, SCROLL_GLIDE_SPEED, dtSeconds), 0f, max);
            graphics.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
            try {
                this.list.extract(graphics, panelX, panelY, panelW, panelH, mouseX, mouseY, dtSeconds, reveal, ScrollGlide.shown(this.scrollY));
                this.list.extractScrollIndicator(graphics, panelX, panelY, panelW, panelH, this.scrollY, reveal);
            } finally {
                graphics.disableScissor();
            }
        }

        @Override
        public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY, final int panelX, final int panelY, final int panelW, final int panelH) {
            final float max = this.list.maxScroll(panelH);
            if (max <= 0f) {
                return false;
            }
            this.scrollTarget = Mth.clamp(this.scrollTarget - (float) scrollY * SCROLL_ROWS * this.list.rowHeight(), 0f, max);
            return true;
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (!this.list.click(event.y(), panelY, panelH, this.scrollY)) {
                return false;
            }
            if (CategoryOverflowButton.this.closePopover != null) {
                CategoryOverflowButton.this.closePopover.run();
            }
            return true;
        }
}
}
