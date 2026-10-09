package main.walksy.lib.core.config.internal;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.api.config.value.WLColor;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.options.category.CategoryTabBar;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.toast.WLToast;
import main.walksy.lib.core.toast.ToastCorner;
import main.walksy.lib.core.toast.WLToastManager;
import main.walksy.lib.api.config.PathUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AppearanceConfig {
    public static final String CATEGORY = "Appearance";
    private static final int PANEL = 0;
    private static final int SIDEBAR = 1;
    private static final int TAB_BAR = 2;
    private static final int TOAST = 3;
    private static final int PERCENT_MIN = 0;
    private static final int PERCENT_MAX = 300;
    private static final int BLUR_PERCENT_MAX = 600;
    private static final int PERCENT_DEFAULT = 100;
    private static final int SHEET_BLUR_DEFAULT = 200;
    private static final int POPOVER_BLUR_DEFAULT = 300;
    private static final int TOAST_STOCK_TINT = 0xFFFFFF;
    private static final int TOAST_OPACITY_MAX = 250;
    private static final int TOAST_TIME_MIN = 25;
    private static final int TOAST_TIME_MAX = 400;
    private static final int UI_SCALE_MIN = 50;
    private static final int UI_SCALE_MAX = 200;
    private int sheetBlur = SHEET_BLUR_DEFAULT;
    private int sidebarBlur = 100;
    private int tabBarBlur = 100;
    private int popoverBlur = POPOVER_BLUR_DEFAULT;
    private int controlBlur = 100;
    private int toastBlur = 100;
    private WLColor lightPanel = new WLColor(Theme.LIGHT.panelTint());
    private WLColor darkPanel = new WLColor(Theme.DARK.panelTint());
    private WLColor lightSidebar = new WLColor(Theme.LIGHT.sidebarTint());
    private WLColor darkSidebar = new WLColor(Theme.DARK.sidebarTint());
    private WLColor lightTabBar = new WLColor(Theme.LIGHT.fieldTint());
    private WLColor darkTabBar = new WLColor(Theme.DARK.fieldTint());
    private WLColor lightToast = new WLColor(TOAST_STOCK_TINT);
    private WLColor darkToast = new WLColor(TOAST_STOCK_TINT);
    private WLColor accent = new WLColor(Theme.DARK.accentRgb());
    private int scrollGlide = 150;
    private UiStyle style = UiStyle.MODERN;
    private UiStyle queuedStyle;
    private Theme queuedTheme;
    private ToastCorner toastCorner = ToastCorner.TOP_RIGHT;
    private int toastOpacity = 100;
    private int toastTime = 100;
    private int uiScale = PERCENT_DEFAULT;
    private Config config;
    private boolean sidebarCategories;
    private boolean focusRing;

    public Config config() {
        if (this.config == null) {
            this.config = Config.builder()
                .path(PathUtils.ofGlobalDir("walksylib"))
                .sharedFile()
                .seedFrom(PathUtils.ofLibraryDir("walksylib"))
                .movedFrom(PathUtils.ofConfigDir("walksylib-appearance"))
                .category(Category.builder(CATEGORY)
                    .library()
                    .group(OptionGroup.builder("General")
                        .option(Options.ofEnum("Style", this::targetStyle, UiStyle.MODERN, this::setStyle, UiStyle.class).build())
                        .option(Options.ofBoolean("Light Mode", this::targetLightMode, true, this::setLightMode).build())
                        .option(Options.ofNumber("UI Scale", () -> this.uiScale, PERCENT_DEFAULT, this::setUiScale).range(UI_SCALE_MIN, UI_SCALE_MAX, 5).build())
                        .option(Options.ofNumber("Scroll Smoothness", () -> this.scrollGlide, 150, v -> this.scrollGlide = v).range(PERCENT_MIN, PERCENT_MAX, 5).description("How long scrolling keeps gliding after you stop").build())
                        .option(Options.ofBoolean("Text Box Focus Ring", this::focusRing, false, v -> this.focusRing = v)
                            .description(OptionDescription.ofText(() -> "Sweep a light around the edge of a text box while it is being typed in."))
                            .build())
                        .build())
                    .group(OptionGroup.builder("Notifications")
                        .option(Options.ofEnum("Corner", this::toastCorner, ToastCorner.TOP_RIGHT, this::setToastCorner, ToastCorner.class)
                            .description(OptionDescription.ofText(() -> "Which corner notifications sit in and animate out of."))
                            .build())
                        .option(Options.ofNumber("Tint Strength", () -> this.toastOpacity, 100, v -> this.toastOpacity = v).range(PERCENT_MIN, TOAST_OPACITY_MAX, 5).saveKey("Blur").description("How strong the notification tint is").build())
                        .option(Options.ofNumber("Time On Screen", () -> this.toastTime, 100, v -> this.toastTime = v).range(TOAST_TIME_MIN, TOAST_TIME_MAX, 5).build())
                        .option(Options.ofButton("Show Me One", this::previewToast).build())
                        .build())
                    .group(OptionGroup.builder("Tint")
                        .option(this.tint("Panel", true, PANEL))
                        .option(this.tint("Side Panels", true, SIDEBAR))
                        .option(this.tint("Tab Bar", true, TAB_BAR))
                        .option(this.tint("Notifications", true, TOAST))
                        .option(this.tint("Panel", false, PANEL))
                        .option(this.tint("Side Panels", false, SIDEBAR))
                        .option(this.tint("Tab Bar", false, TAB_BAR))
                        .option(this.tint("Notifications", false, TOAST))
                        .build())
                    .group(OptionGroup.builder("Accent")
                        .option(Options.ofColor("Accent Colour", () -> this.accent, new WLColor(Theme.DARK.accentRgb()), this::setAccent).disableAlpha().disableChroma().build())
                        .build())
                    .group(OptionGroup.builder("Blur")
                        .option(this.blur("Background", () -> this.sheetBlur, v -> this.sheetBlur = v, SHEET_BLUR_DEFAULT, "How frosted the world behind the panel is"))
                        .option(this.blur("Side Panels", () -> this.sidebarBlur, v -> this.sidebarBlur = v, "The mods list and category rail"))
                        .option(this.blur("Tab Bar", () -> this.tabBarBlur, v -> this.tabBarBlur = v, "The category strip, search bar and Reset button"))
                        .option(this.blur("Popovers", () -> this.popoverBlur, v -> this.popoverBlur = v, POPOVER_BLUR_DEFAULT, "Dropdowns, color pickers and other floating panels"))
                        .option(this.blur("Controls", () -> this.controlBlur, v -> this.controlBlur = v, "Switches, sliders and text boxes"))
                        .option(this.blur("Notifications", () -> this.toastBlur, v -> this.toastBlur = v, "The notification cards"))
                        .build())
                    .build())
                .build();
            this.config.load();
        }
        return this.config;
    }
    

    public void save() {
        if (this.config != null) {
            this.config.save();
        }
    }

    private Option<WLColor> tint(final String name, final boolean light, final int surface) {
        final WLColor stock = new WLColor(stockTint(light ? Theme.LIGHT : Theme.DARK, surface));
        return Options.ofColor(name, () -> this.tintFor(light, surface), stock, v -> this.setTint(light, surface, v))
            .saveKey((light ? "Light " : "Dark ") + name)
            .visibility(() -> this.lightMode() == light)
            .disableAlpha()
            .build();
    }

    private Option<Integer> blur(final String name, final Supplier<Integer> get, final Consumer<Integer> set, final int defaultValue, final String description) {
        return Options.ofNumber(name, get, defaultValue, set).range(PERCENT_MIN, BLUR_PERCENT_MAX, 5).description(description).build();
    }

    private Option<Integer> blur(final String name, final Supplier<Integer> get, final Consumer<Integer> set, final String description) {
        return this.blur(name, get, set, PERCENT_DEFAULT, description);
    }

    private static int stockTint(final Theme theme, final int surface) {
        return switch (surface) {
            case SIDEBAR -> theme.sidebarTint();
            case TAB_BAR -> theme.fieldTint();
            case TOAST -> TOAST_STOCK_TINT;
            default -> theme.panelTint();
        };
    }

    private WLColor tintFor(final boolean light, final int surface) {
        return switch (surface) {
            case SIDEBAR -> light ? this.lightSidebar : this.darkSidebar;
            case TAB_BAR -> light ? this.lightTabBar : this.darkTabBar;
            case TOAST -> light ? this.lightToast : this.darkToast;
            default -> light ? this.lightPanel : this.darkPanel;
        };
    }

    private void setTint(final boolean light, final int surface, final WLColor value) {
        switch (surface) {
            case SIDEBAR -> {
                if (light) {
                    this.lightSidebar = value;
                } else {
                    this.darkSidebar = value;
                }
            }
            case TAB_BAR -> {
                if (light) {
                    this.lightTabBar = value;
                } else {
                    this.darkTabBar = value;
                }
            }
            case TOAST -> {
                if (light) {
                    this.lightToast = value;
                } else {
                    this.darkToast = value;
                }
            }
            default -> {
                if (light) {
                    this.lightPanel = value;
                } else {
                    this.darkPanel = value;
                }
            }
        }
    }

    private void previewToast() {
        WalksyLib.getInstance()
            .getToastManager()
            .submit(WLToast.of("Notifications", "This is what a toast looks like!").info());
    }

    public void setAccent(final WLColor value) {
        value.setRainbow(false);
        this.accent = value;
        Theme.setAccentOverride(0xFF000000 | rgb(value));
        WalksyLibPanelScreen.applyAppearance();
    }

    public int accent() {
        return rgb(this.accent);
    }

    private static int rgb(final WLColor color) {
        return (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    public float uiScale() {
        return this.uiScale / 100f;
    }

    private void setUiScale(final int value) {
        final int clamped = Math.max(UI_SCALE_MIN, Math.min(UI_SCALE_MAX, value));
        if (this.uiScale == clamped) {
            return;
        }
        this.uiScale = clamped;
        WalksyLibPanelScreen.applyAppearance();
    }

    public UiStyle style() {
        return this.style;
    }

    public UiStyle targetStyle() {
        return this.queuedStyle != null ? this.queuedStyle : this.style;
    }

    private void setStyle(final UiStyle value) {
        final UiStyle next = value == null ? UiStyle.MODERN : value;
        if (this.targetStyle() == next) {
            return;
        }
        this.queuedStyle = next;
        if (WalksyLibPanelScreen.swapAppearance(() -> this.applyStyle(next))) {
            return;
        }
        this.applyStyle(next);
        WalksyLibPanelScreen.applyAppearance();
    }

    private void applyStyle(final UiStyle value) {
        this.style = value;
        if (this.queuedStyle == value) {
            this.queuedStyle = null;
        }
    }

    public boolean focusRing() {
        return this.focusRing;
    }

    public boolean sidebarCategories() {
        return this.sidebarCategories;
    }

    public void setSidebarCategories(final boolean value) {
        if (this.sidebarCategories == value) {
            return;
        }
        this.sidebarCategories = value;
        WalksyLibPanelScreen.applyAppearance();
    }

    public int panelTint() {
        return rgb(this.tintFor(this.lightMode(), PANEL));
    }

    public int sidebarTint() {
        return rgb(this.tintFor(this.lightMode(), SIDEBAR));
    }

    public int tabBarTint() {
        return rgb(this.tintFor(this.lightMode(), TAB_BAR));
    }

    public int toastTint() {
        return rgb(this.tintFor(this.lightMode(), TOAST));
    }

    public float toastOpacityScale() {
        return this.toastOpacity / 100f;
    }

    public float toastTimeScale() {
        return this.toastTime / 100f;
    }

    public ToastCorner toastCorner() {
        return this.toastCorner;
    }

    private void setToastCorner(final ToastCorner value) {
        this.toastCorner = value == null ? ToastCorner.TOP_RIGHT : value;
    }

    public boolean lightMode() {
        return WalksyLibPanelScreen.THEME != Theme.DARK;
    }

    private boolean targetLightMode() {
        return (this.queuedTheme != null ? this.queuedTheme : WalksyLibPanelScreen.THEME) != Theme.DARK;
    }

    public void setLightMode(final boolean light) {
        final Theme wanted = light ? Theme.LIGHT : Theme.DARK;
        if (this.targetLightMode() == light) {
            return;
        }
        this.queuedTheme = wanted;
        if (WalksyLibPanelScreen.swapAppearance(() -> this.applyTheme(wanted))) {
            return;
        }
        this.applyTheme(wanted);
        WalksyLibPanelScreen.applyAppearance();
    }

    private void applyTheme(final Theme theme) {
        WalksyLibPanelScreen.THEME = theme;
        if (this.queuedTheme == theme) {
            this.queuedTheme = null;
        }
    }

    private int factorForLayer(final int layer) {
        if (layer <= WalksyLibPanelScreen.PANEL_LAYER) {
            return this.sheetBlur;
        }
        if (layer == WalksyLibPanelScreen.SIDEBAR_LAYER) {
            return this.sidebarBlur;
        }
        if (layer == CategoryTabBar.BAR_LAYER) {
            return this.tabBarBlur;
        }
        if (layer >= WLToastManager.PANEL_LAYER) {
            return this.toastBlur;
        }
        if (layer >= PopoverHost.PANEL_LAYER) {
            return this.popoverBlur;
        }
        return this.controlBlur;
    }

    public int blurRadius(final int authored, final int layer) {
        return scaleRadius(authored, this.factorForLayer(layer));
    }

    private static int scaleRadius(final int authored, final int percent) {
        if (authored <= 0) {
            return 0;
        }
        if (percent == 100) {
            return authored;
        }
        final int scaled = Math.round(authored * percent / 100f);
        return Math.max(percent <= 0 ? 0 : 1, Math.min(LiquidGlassConstants.MAX_BLUR_RADIUS, scaled));
    }

    public int scrollGlidePercent() {
        return this.scrollGlide;
    }

    public float scrollGlideSpeed(final float authoredRate) {
        if (this.scrollGlide <= 0) {
            return 0f;
        }
        if (this.scrollGlide == 100) {
            return authoredRate;
        }
        return authoredRate * 100f / this.scrollGlide;
    }
}
