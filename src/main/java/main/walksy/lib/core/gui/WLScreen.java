package main.walksy.lib.core.gui;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.render.perf.PerformanceMonitorWindow;
import main.walksy.lib.core.render.perf.UiProfiler;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.screen.ScreenBridge;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

public abstract class WLScreen extends ScreenBridge {
    public static final int POINTER_AWAY = -10_000;
    public static final float TARGET_GUI_SCALE = 2f;

    private static final float REFERENCE_WINDOW_WIDTH = 2560f;
    private static final float REFERENCE_WINDOW_HEIGHT = 1440f;

    private static final float MIN_WINDOW_FIT = 0.25f;
    private static final float BASE_UI_SCALE = 1.42f;
    private static final float WARM_OPACITY = 0.004f;

    protected final Screen parent;
    private boolean contentTransformActive;
    private boolean warmedWidgets;

    protected WLScreen(final Screen parent, final String title) {
        super(Component.literal(title));
        this.parent = parent;
        WalksyLib.getInstance().getOfflineRegistries().warm();
    }

    @Override
    public void removed() {
        WLRenderSystem.cursors().reset();
        super.removed();
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            WLClient.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected final void renderFrame(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float gameDelta) {
        final float delta = WLRenderSystem.frameDeltaTicks();
        WLRenderSystem.beginScreenFrame();
        WalksyLib.getInstance().getOfflineRegistries().bindIfReady();
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.begin(UiProfiler.Section.SCREEN);
        }
        this.extract(graphics, mouseX, mouseY, delta);
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.SCREEN);
        }
        final boolean warming = !this.warmedWidgets;
        final float opacity = warming ? Math.max(this.contentOpacity(), WARM_OPACITY) : this.contentOpacity();
        this.warmedWidgets = true;
        for (final GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                widget.setAlpha(opacity);
            }
        }
        final boolean pointerInsidePopover = PopoverHost.anyPanelContains(mouseX, mouseY) || (WalksyLibConstants.DEV_BUILD && PerformanceMonitorWindow.get().contains(mouseX, mouseY));
        final int belowMouseX = pointerInsidePopover ? POINTER_AWAY : mouseX;
        final int belowMouseY = pointerInsidePopover ? POINTER_AWAY : mouseY;
        final boolean animating = warming || this.isAnimating();
        if (animating) {
            WLRenderSystem.beginMotion();
        }
        final Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        this.applyContentTransform(pose);
        this.contentTransformActive = true;
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.begin(UiProfiler.Section.WIDGETS);
        }
        try {
            this.renderChildren(graphics, belowMouseX, belowMouseY, delta);
        } finally {
            this.contentTransformActive = false;
        }
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.WIDGETS);
        }
        pose.popMatrix();
        if (animating) {
            WLRenderSystem.endMotion();
        }
        this.extractOverlay(graphics, mouseX, mouseY, delta);
        WLRenderSystem.endScreenFrame(graphics);
        if (warming) {
            WLRenderSystem.discardFrameTime();
        }
    }

    public final void warmFrame(final WLGraphics2D graphics) {
        this.prepareWarmFrame();
        this.renderFrame(graphics, POINTER_AWAY, POINTER_AWAY, 0f);
    }

    protected void prepareWarmFrame() {
    }

    public static float uiScaleFactor() {
        final Window window = Minecraft.getInstance().getWindow();
        return (TARGET_GUI_SCALE / (float) window.getGuiScale()) * windowFit(window) * sizeMultiplier();
    }

    public static float sizeMultiplier() {
        return BASE_UI_SCALE * userScale();
    }

    private static float userScale() {
        final WalksyLib instance = WalksyLib.getInstanceOrNull();
        return instance == null ? 1f : instance.getAppearancePage().uiScale();
    }

    private static float windowFit(final Window window) {
        final float width = Math.max(1, window.getWidth());
        final float height = Math.max(1, window.getHeight());
        final float fit = Math.min(width / REFERENCE_WINDOW_WIDTH, height / REFERENCE_WINDOW_HEIGHT);
        return Math.max(MIN_WINDOW_FIT, fit);
    }

    public static float hairlinePx() {
        return 1f / DevicePixels.perLogicalY();
    }

    public static float snapPhysical(final float logical) {
        return DevicePixels.snapY(logical);
    }

    protected void extractOverlay(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
    }

    protected void applyContentTransform(final Matrix3x2fStack pose) {}

    protected final boolean isContentTransformActive() {
        return this.contentTransformActive;
    }

    protected float contentOpacity() {
        return 1f;
    }

    protected boolean isAnimating() {
        return false;
    }

    public abstract void extract(WLGraphics2D graphics, int mouseX, int mouseY, float delta);
}
