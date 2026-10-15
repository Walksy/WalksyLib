package main.walksy.lib.core.gui;

import main.walksy.lib.core.platform.WLClient;

import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.gui.liquid.LiquidProfile;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.liquid.LiquidTransition;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.config.internal.AppearanceConfig;
import main.walksy.lib.core.config.internal.PerformanceConfig;
import main.walksy.lib.core.render.perf.PerformanceMonitorWindow;
import main.walksy.lib.core.config.serialization.SpriteCache;
import main.walksy.lib.core.teams.GlobalTeams;
import main.walksy.lib.core.gui.modal.ConfirmPopover;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.hud.HudEditScreen;
import main.walksy.lib.core.hud.HudSpace;
import main.walksy.lib.core.keybinds.KeyBinds;
import main.walksy.lib.core.config.internal.LogsConfig;
import main.walksy.lib.core.config.internal.ChangelogConfig;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.gui.widget.CategoryHolder;
import main.walksy.lib.core.gui.widget.SidebarActionRow;
import main.walksy.lib.core.gui.widget.options.AllModsPaneWidget;
import main.walksy.lib.core.gui.widget.options.ChangelogPaneWidget;
import main.walksy.lib.core.gui.widget.options.UpdateHeroWidget;
import main.walksy.lib.core.gui.widget.options.SuggestionPaneWidget;
import main.walksy.lib.core.mods.remote.ModUpdate;
import main.walksy.lib.core.mods.remote.ModSuggestion;
import main.walksy.lib.core.config.internal.UpdateConfig;
import main.walksy.lib.core.gui.widget.LinkButton;
import main.walksy.lib.core.gui.widget.ModWidget;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.ScrollPanel;
import main.walksy.lib.core.gui.widget.SearchBar;
import main.walksy.lib.core.gui.widget.options.category.CategoryTabBar;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.gui.widget.options.category.CategoryOverflowButton;
import main.walksy.lib.core.gui.widget.options.category.CategorySidebarWidget;
import main.walksy.lib.core.gui.widget.options.EmptyStateWidget;
import main.walksy.lib.core.gui.widget.options.ModUpdateBanner;
import main.walksy.lib.core.gui.widget.options.OptionWidgetFactory;
import main.walksy.lib.core.gui.widget.options.types.KeybindOptionWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.gui.animation.ScreenAnimation;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.style.UiStyle;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public class WalksyLibPanelScreen extends WLScreen {
    public static Theme THEME = Theme.LIGHT;
    public static final int PANEL_LAYER = 0;
    private static final float REST_TOLERANCE_PX = 0.25f;
    private static final float PANEL_CORNER_RADIUS = 18f;

    private static final boolean FLAT_PANEL_STYLE = false;
    private static final float FLAT_PANEL_OPACITY = 0.8f;

    public static final int SIDEBAR_LAYER = 5;
    private static final int SIDEBAR_MARGIN = 8;
    private static final int SIDEBAR_PADDING = 12;
    private static final float CONTENT_SCROLLBAR_END_INSET = 10f;
    private static final float SIDEBAR_CORNER_RADIUS = PANEL_CORNER_RADIUS - SIDEBAR_MARGIN;
    public static final int SEARCH_BAR_HEIGHT = 18;
    private static final int SEARCH_BAR_WIDTH = 160;
    private static final int RESET_SEARCH_GAP = 8;
    private static final float MODS_HEADER_SIZE = Type.SUBHEADLINE.size();
    private static final float EMPTY_MODS_SIZE = Type.FOOTNOTE.size();
    private static final int MODS_HEADER_HEIGHT = 6;
    private static final int MODS_HEADER_GAP = 8;
    private static final int MOD_ROW_HEIGHT = 20;
    private static final int MOD_ROW_GAP = 2;
    private static final int SIDEBAR_CONTENT_TOP_INSET = 18;
    private static final int SAVE_ROW_GAP = 8;
    private static final float MOD_TITLE_SIZE = Type.TITLE2.size();
    private static final float TITLE_CAP_HEIGHT_RATIO = 0.59043f;
    private static final float MOD_SUBTITLE_SIZE = Type.SUBHEADLINE.size();
    private static final float MOD_AUTHOR_SIZE = Type.CAPTION1.size();
    private static final int TITLE_SUBTITLE_GAP = 2;
    private static final int TITLE_AUTHOR_GAP = 6;
    private static final int AUTHOR_LINK_GAP = 8;
    private static final int LINK_GAP = 4;
    private static final float LINK_BUTTON_RATIO = 1.05f;
    private static final int TITLE_TABS_GAP = 10;
    private static final int ALL_MODS_CONTENT_TOP_INSET = 8;

    private static final int TAB_EDGE_LIFT = 4;
    private static final int TAB_HEIGHT = 26;
    private static final float SMALL_CHROME_GLARE_RANGE = 11f;
    private final LiquidTransition transition;
    private final LiquidRect origin;
    private final GlassStyle originStyle;
    private int targetX;
    private int targetY;
    private int targetW;
    private int targetH;
    private int sidebarX;
    private int sidebarY;
    private int sidebarW;
    private int sidebarH;
    private static final float RAIL_INSET = 48f;
    private boolean initialized;
    private int lastInitWidth = -1;
    private int lastInitHeight = -1;
    private SearchBar searchBar;
    private ResetButton resetButton;
    private String searchQuery = "";
    private SidebarActionRow editHudRow;
    private boolean showingAllMods;
    private static final String SECTION_MODS = "Mods";
    private static final String SECTION_LIBRARY = "WalksyLib";
    private static final String SECTION_DEV = "Dev";
    private static final String UPDATE_ROW_LABEL = "Update Available";
    private static final int UPDATE_ROW_PALETTE_INDEX = 3;
    private static final float UPDATE_ROW_DARKEN = 0.22f;
    private static WalksyLibPanelScreen active;
    private float panelCornerRadius = PANEL_CORNER_RADIUS;
    private float sidebarCornerRadius = SIDEBAR_CORNER_RADIUS;
    private float uiScale = 1f;
    private Mod selectedMod;
    private Category selectedCategory;
    private CategoryTabBar tabBar;
    private CategoryOverflowButton categoryOverflow;
    private CategorySidebarWidget categoryRail;
    private List<Category> lastRailCategories = List.of();
    private boolean firstLayout = true;
    private boolean warming;
    private long animatedFrame = -1L;
    private final List<AbstractWidget> detailWidgets = new ArrayList<>();
    private final PopoverHost activePopover = new PopoverHost();
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private PressTarget pressTarget = PressTarget.NONE;
    private String preselectModId;
    private String preselectLibraryPage;
    private boolean pendingShowAllMods;
    private int pendingAppearanceRebuild = -1;
    private Runnable pendingSwap;
    private boolean swapClosesPopover;
    private float appearanceSwap = 1f;
    private float shellMinecraft;
    private GlassStyle swapPanelFrom;
    private GlassStyle swapSidebarFrom;
    private static final float APPEARANCE_SWAP_SPEED = 0.12f;
    private static final float APPEARANCE_SWAP_FLIP = 0.5f;
    private static final int APPEARANCE_REBUILD_DELAY_TICKS = 7;
    private static final float PANEL_FRACTION = 0.6085f;
    private static final float MAX_PANEL_FRACTION = 0.96f;
    private Mod library;
    private Mod appearanceLibraryMod;
    private Mod keyBindsLibraryMod;
    private Mod logsLibraryMod;
    private Mod changelogLibraryMod;
    private Mod updateLibraryMod;
    private boolean builtWithUpdateRow;
    private boolean builtWithBanner;
    private Mod devPerformanceMod;
    private static final float OVERFLOW_GAP = 6f;

    private static GlassStyle panelSheetStyle() {
        return new GlassStyle()
            .layer(PANEL_LAYER)
            .blurRadius(LiquidGlassConstants.BLUR_PANEL)
            .tint(WalksyLib.getInstance().getAppearancePage().panelTint(), Theme.get().panelTintAlpha())
            .toneRange(Theme.get().toneFloor(), Theme.get().toneCeiling())
            .chroma(Theme.get().chroma())
            .refractionReach(0.34f)
            .refractionThickness(6f)
            .refractionFactor(1.6f)
            .refractionDispersion(0f)
            .lensFold(1f)
            .refractionShift(0f)
            .fresnelFactor(1.5f)
            .glareFactor(6f)
            .glareRange(34f)
            .glareHardness(9f)
            .brightEdge(0f)
            .edgeDarken(Theme.get().edgeDarkenColor(), Theme.get().edgeDarken())
            .rimLight(Theme.get().rimLightColor(), Theme.get().rimLightPanel())
            .outline(Theme.get().outlineWidth(), Theme.get().shadowColor(), Theme.get().outlineAlpha());
    }

    private static GlassStyle flatPanelLook() {
        return new GlassStyle()
            .layer(PANEL_LAYER)
            .blurRadius(4)
            .tint(Theme.get().flatSurface(), FLAT_PANEL_OPACITY / 0.8f)
            .refractionStrength(0f)
            .refractionThickness(0f)
            .refractionFactor(0f)
            .refractionDispersion(0f)
            .fresnelFactor(0f)
            .glareFactor(0f)
            .brightEdge(0f)
            .shadow(Theme.get().shadowExpand() * 2.5f, Theme.get().shadowFactor(), 0f, Theme.get().shadowOffsetY() * 2f)
            .shadowColor(Theme.get().shadowColor(), Theme.get().shadowAlpha());
    }

    public static GlassStyle sidebarStyle() {
        return new GlassStyle()
            .layer(SIDEBAR_LAYER)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .tint(chromeTintColor(), chromeTintAlpha())
            .refractionStrength(0.4f)
            .refractionThickness(4f)
            .refractionFactor(1.2f)
            .refractionDispersion(1f)
            .brightEdge(LiquidGlassConstants.CHROME_BRIGHT_EDGE)
            .fresnelFactor(1.5f)
            .glareFactor(Theme.get().chromeGlareFactor())
            .glareRange(34f)
            .glareHardness(9f)
            .rimLight(Theme.get().rimLightColor(), Theme.get().rimLightChrome())
            .edgeDarken(Theme.get().edgeDarkenColor(), Theme.get().chromeEdgeDarken())
            .standardShadow();
    }

    public static GlassStyle sidebarPanelStyle() {
        return LiquidKnob.bevelKnobStyle(SIDEBAR_LAYER)
            .blurRadius(sidebarStyle().getBlurRadius())
            .tint(chromeTintColor(), chromeTintAlpha());
    }

    public static GlassStyle floatingChromeStyle() {
        return sidebarStyle().copy().glareRange(SMALL_CHROME_GLARE_RANGE);
    }

    public static GlassStyle panelStyle() {
        return (FLAT_PANEL_STYLE ? flatPanelLook() : panelSheetStyle()).copy();
    }

    private static int modsHeaderColor() {
        return Theme.get().secondaryLabel();
    }

    public static int chromeTintColor() {
        return WalksyLib.getInstance().getAppearancePage().sidebarTint();
    }

    public static float chromeTintAlpha() {
        return Theme.get().sidebarTintAlpha();
    }

    public static int fieldTintColor() {
        return WalksyLib.getInstance().getAppearancePage().tabBarTint();
    }

    public static float fieldTintAlpha() {
        return Theme.get().fieldTintAlpha();
    }

    public WalksyLibPanelScreen(final Screen parent, final String title) {
        this(parent, title, null, null);
    }

    public WalksyLibPanelScreen(final Screen parent, final String title, final LiquidRect origin, final GlassStyle originStyle) {
        super(parent, title);
        this.origin = origin;
        this.originStyle = originStyle;
        this.transition = new LiquidTransition(LiquidProfile.smooth(), 0.5f);
    }

    public WalksyLibPanelScreen preselect(final String modId) {
        this.preselectModId = modId;
        return this;
    }

    public WalksyLibPanelScreen openOnAllMods() {
        this.pendingShowAllMods = true;
        return this;
    }
    public WalksyLibPanelScreen preselectLibraryPage(final String category) {
        this.preselectLibraryPage = category;
        return this;
    }

    @Override
    public void resize(final int width, final int height) {
        if (width == this.width && height == this.height) {
            return;
        }
        super.resize(width, height);
    }

    public static boolean swapAppearance(final Runnable apply) {
        if (active == null || active.transition.isClosing() || active.targetW <= 0 || active.targetH <= 0) {
            return false;
        }
        active.beginSwap(apply, true);
        return true;
    }

    private void beginSwap(final Runnable apply, final boolean closePopover) {
        if (this.pendingSwap != null) {
            final Runnable previous = this.pendingSwap;
            this.pendingSwap = () -> {
                previous.run();
                apply.run();
            };
        } else {
            this.pendingSwap = apply;
            this.appearanceSwap = this.appearanceSwap >= 1f ? 0f : Math.min(this.appearanceSwap, 1f - this.appearanceSwap);
        }
        if (closePopover) {
            this.swapClosesPopover = true;
            this.activePopover.close();
        }
    }

    private void finishSwap() {
        final Runnable apply = this.pendingSwap;
        this.pendingSwap = null;
        this.swapClosesPopover = false;
        this.pendingAppearanceRebuild = -1;
        this.swapPanelFrom = this.panelShellStyle();
        this.swapSidebarFrom = this.sidebarShellStyle();
        final LiquidRect before = this.getBounds();
        apply.run();
        this.rebuildWidgets();
        final LiquidRect after = this.getBounds();
        if (!before.equals(after)) {
            this.transition.reshape(before, after);
        }
    }

    private void applyPendingSwap() {
        if (this.pendingSwap == null) {
            return;
        }
        final Runnable apply = this.pendingSwap;
        this.pendingSwap = null;
        this.swapClosesPopover = false;
        apply.run();
    }

    private void advanceAppearanceSwap(final float delta) {
        if (this.appearanceSwap >= 1f) {
            this.shellMinecraft = UiStyle.minecraft() ? 1f : 0f;
            this.swapPanelFrom = null;
            this.swapSidebarFrom = null;
            return;
        }
        final float step = APPEARANCE_SWAP_SPEED * Math.max(0f, delta);
        this.appearanceSwap = Math.min(this.pendingSwap != null ? APPEARANCE_SWAP_FLIP : 1f, this.appearanceSwap + step);
        final float gap = (WalksyLib.getInstance().getAppearancePage().targetStyle() == UiStyle.MINECRAFT ? 1f : 0f) - this.shellMinecraft;
        this.shellMinecraft += Math.signum(gap) * Math.min(step, Math.abs(gap));
    }

    private float swapContentAlpha() {
        if (this.appearanceSwap >= 1f) {
            return 1f;
        }
        final float d = Math.abs(2f * this.appearanceSwap - 1f);
        return d * d * (3f - 2f * d);
    }

    private float shellMinecraftAmount() {
        if (this.appearanceSwap >= 1f) {
            return -1f;
        }
        final float m = this.shellMinecraft;
        return m * m * (3f - 2f * m);
    }

    private GlassStyle swapBlend(final GlassStyle from, final GlassStyle live) {
        if (from == null || this.pendingSwap != null || this.appearanceSwap >= 1f) {
            return live;
        }
        final float t = Mth.clamp((this.appearanceSwap - APPEARANCE_SWAP_FLIP) / (1f - APPEARANCE_SWAP_FLIP), 0f, 1f);
        return GlassStyle.lerp(from, live, t * t * (3f - 2f * t));
    }

    private GlassStyle panelShellStyle() {
        final GlassStyle basePanelStyle = FLAT_PANEL_STYLE ? flatPanelLook() : panelSheetStyle();
        final GlassStyle style = this.originStyle == null ? basePanelStyle : GlassStyle.lerp(this.originStyle, basePanelStyle, this.transition.value());
        if (this.originStyle != null) {
            style.blurRadius(this.transition.value() < 0.5f ? this.originStyle.getBlurRadius() : basePanelStyle.getBlurRadius());
        }
        return style.scaled(this.uiScale);
    }

    private GlassStyle sidebarShellStyle() {
        return sidebarPanelStyle().scaled(this.uiScale);
    }

    public static void applyAppearance() {
        if (active != null) {
            active.pendingAppearanceRebuild = APPEARANCE_REBUILD_DELAY_TICKS;
        }
    }

    @Override
    public void removed() {
        this.applyPendingSwap();
        this.activePopover.closeInstant();
        super.removed();
        if (active == this) {
            active = null;
        }
    }

    @Override
    protected void init() {
        super.init();
        final boolean sizeChanged = this.width != this.lastInitWidth || this.height != this.lastInitHeight;
        this.lastInitWidth = this.width;
        this.lastInitHeight = this.height;
        if (this.initialized && sizeChanged) {
            this.activePopover.close();
        }
        final float scale = WLScreen.uiScaleFactor();
        this.uiScale = scale;
        this.panelCornerRadius = PANEL_CORNER_RADIUS * scale;
        this.sidebarCornerRadius = SIDEBAR_CORNER_RADIUS * scale;
        final int sidebarMargin = Math.round(SIDEBAR_MARGIN * scale);
        final int sidebarPadding = Math.round(SIDEBAR_PADDING * scale);
        final int searchBarHeight = Math.round(SEARCH_BAR_HEIGHT * scale);
        final int searchBarWidth = Math.round(SEARCH_BAR_WIDTH * scale);
        final int resetSearchGap = Math.round(RESET_SEARCH_GAP * scale);
        final float modsHeaderSize = MODS_HEADER_SIZE * scale;
        final float emptyModsSize = EMPTY_MODS_SIZE * scale;
        final int modsHeaderHeight = Math.round(MODS_HEADER_HEIGHT * scale);
        final int modsHeaderGap = Math.round(MODS_HEADER_GAP * scale);
        final int modRowHeight = Math.round(MOD_ROW_HEIGHT * scale);
        final int modRowGap = Math.round(MOD_ROW_GAP * scale);
        final int sectionGap = modsHeaderGap * 2;
        final int sidebarContentTopInset = Math.round(SIDEBAR_CONTENT_TOP_INSET * scale);
        final float panelFraction = Math.min(MAX_PANEL_FRACTION, PANEL_FRACTION * WLScreen.sizeMultiplier());
        this.targetW = (int) (this.width * panelFraction);
        this.targetH = (int) (this.height * panelFraction);
        this.targetX = (this.width - this.targetW) / 2;
        this.targetY = (this.height - this.targetH) / 2;
        if (!this.initialized) {
            this.transition.reset();
            this.initialized = true;
        }
        if (this.origin != null) {
            this.transition.setEndpoints(this.origin, this.getBounds());
            this.transition.expand();
        } else {
            this.transition.open(this.getBounds());
        }
        final float sidebarMin = Math.min(128f * scale, this.targetW * 0.32f);
        this.sidebarW = (int) Mth.clamp(this.targetW * 0.208f, sidebarMin, 208f * scale);
        this.sidebarX = this.targetX + sidebarMargin;
        this.sidebarY = this.targetY + sidebarMargin;
        this.sidebarH = this.targetH - sidebarMargin * 2;
        final int searchX = this.targetX + this.targetW - sidebarMargin - searchBarWidth;
        final int searchY = this.targetY + sidebarMargin;
        this.searchBar = new SearchBar(searchX, searchY, searchBarWidth, searchBarHeight, this::applySearch).setLiftedText(true);
        this.searchBar.setValue(this.searchQuery);
        this.addRenderableWidget(this.searchBar);
        final int resetButtonWidth = ResetButton.preferredWidth("Reset", searchBarHeight);
        final int resetX = searchX - resetSearchGap - resetButtonWidth;
        this.resetButton = new ResetButton(resetX, searchY, resetButtonWidth, searchBarHeight, "Reset", this::askToReset);
        this.updateResetButtonVisibility();
        this.addRenderableWidget(this.resetButton);
        final int sidebarContentX = this.sidebarX + sidebarPadding;
        final int sidebarContentW = this.sidebarW - sidebarPadding * 2;
        final ScrollPanel sidebarScroll = new ScrollPanel(sidebarContentX, this.sidebarY, sidebarContentW, Math.max(1, this.sidebarH));
        sidebarScroll.setContentInsetTop(sidebarContentTopInset);
        sidebarScroll.setScrollbarInsets(SIDEBAR_PADDING + 2f, SIDEBAR_PADDING - 3f);
        this.builtWithUpdateRow = WalksyLib.getInstance().getModUpdates().library().isOutdated();
        final Mod update = this.builtWithUpdateRow ? this.updateMod() : null;
        if (update != null) {
            sidebarScroll.addChild(0, (x, y, w) -> new ModWidget(update, this::selectMod, () -> this.selectedMod, UPDATE_ROW_LABEL, Icons.UPDATE, x, y, w, modRowHeight).labelColor(WalksyLibPanelScreen::updateRowColor));
        }
        sidebarScroll.addChild(update == null ? 0 : sectionGap, (x, y, w) -> new CategoryHolder(x, y, w, modsHeaderHeight, SECTION_MODS, modsHeaderSize, modsHeaderColor()));
        boolean firstRow = true;
        for (final Mod mod : WalksyLib.getInstance().getModEntryPointList().getMods()) {
            sidebarScroll.addChild(firstRow ? modsHeaderGap : modRowGap, (x, y, w) -> new ModWidget(mod, this::selectMod, () -> this.selectedMod, x, y, w, modRowHeight).withUpdateMarker());
            firstRow = false;
        }
        if (firstRow) {
            sidebarScroll.addChild(modsHeaderGap, (x, y, w) -> new CategoryHolder(x, y, w, modRowHeight, "No mods found", emptyModsSize, modsHeaderColor()));
        }
        final Mod library = this.libraryMod();
        if (library != null) {
            sidebarScroll.addChild(sectionGap, (x, y, w) -> new CategoryHolder(x, y, w, modsHeaderHeight, SECTION_LIBRARY, modsHeaderSize, modsHeaderColor()));
            sidebarScroll.addChild(modsHeaderGap, (x, y, w) -> new ModWidget(library, this::selectMod, () -> this.selectedMod, GlobalTeams.CATEGORY, Icons.TEAM, x, y, w, modRowHeight));
            final Mod appearance = this.appearanceMod();
            if (appearance != null) {
                sidebarScroll.addChild(modRowGap, (x, y, w) -> new ModWidget(appearance, this::selectMod, () -> this.selectedMod, AppearanceConfig.CATEGORY, Icons.APPEARANCE, x, y, w, modRowHeight));
            }
            final Mod keyBinds = this.keyBindsMod();
            if (keyBinds != null) {
                sidebarScroll.addChild(modRowGap, (x, y, w) -> new ModWidget(keyBinds, this::selectMod, () -> this.selectedMod, KeyBinds.CATEGORY, Icons.KEYBOARD, x, y, w, modRowHeight));
            }
            final Mod changelog = this.changelogMod();
            if (changelog != null) {
                sidebarScroll.addChild(modRowGap, (x, y, w) -> new ModWidget(changelog, this::selectMod, () -> this.selectedMod, ChangelogConfig.PAGE, Icons.CHANGELOG, x, y, w, modRowHeight));
            }
            sidebarScroll.addChild(modRowGap, (x, y, w) -> new SidebarActionRow(x, y, w, modRowHeight, "All Mods", Icons.ALL_MODS, this::showAllMods, () -> this.showingAllMods));
        }
        sidebarScroll.addChild(library != null ? modRowGap : sectionGap, (x, y, w) -> {
            this.editHudRow = new SidebarActionRow(x, y, w, modRowHeight, "Edit HUD", Icons.HUD, this::openHudEditor, null);
            this.editHudRow.active = HudEditScreen.hasAnything();
            return this.editHudRow;
        });
        if (WalksyLibConstants.DEV_BUILD) {
            final Mod dev = this.devMod();
            if (dev != null) {
                sidebarScroll.addChild(sectionGap, (x, y, w) -> new CategoryHolder(x, y, w, modsHeaderHeight, SECTION_DEV, modsHeaderSize, modsHeaderColor()));
                sidebarScroll.addChild(modsHeaderGap, (x, y, w) -> new ModWidget(dev, this::selectMod, () -> this.selectedMod, PerformanceConfig.CATEGORY, Icons.PULSE, x, y, w, modRowHeight));
            }
        }
        sidebarScroll.relayout();
        this.addRenderableWidget(sidebarScroll);
        active = this;
        this.contentX = this.sidebarX + this.sidebarW + sidebarMargin;
        this.contentY = this.sidebarY;
        this.contentW = (this.targetX + this.targetW - sidebarMargin) - this.contentX;
        this.contentH = (this.targetY + this.targetH) - this.contentY;
        Mod toSelect = null;
        if (this.preselectModId != null && this.selectedMod == null && !this.showingAllMods) {
            for (final Mod candidate : WalksyLib.getInstance().getModEntryPointList().getMods()) {
                if (this.preselectModId.equals(candidate.getContainer().getMetadata().getId())) {
                    toSelect = candidate;
                    break;
                }
            }
            this.preselectModId = null;
        }
        if (toSelect == null && this.preselectLibraryPage != null && this.selectedMod == null && !this.showingAllMods) {
            toSelect = this.libraryPageMod(this.preselectLibraryPage);
            this.preselectLibraryPage = null;
        }
        if (toSelect != null) {
            this.selectMod(toSelect);
        } else if (this.pendingShowAllMods && this.selectedMod == null) {
            this.pendingShowAllMods = false;
            this.showingAllMods = true;
            this.rebuildDetailPane();
        } else {
            this.pendingShowAllMods = false;
            this.rebuildDetailPane();
        }
    }

    private Mod libraryPageMod(final String category) {
        if (GlobalTeams.CATEGORY.equals(category)) {
            return this.libraryMod();
        }
        if (AppearanceConfig.CATEGORY.equals(category)) {
            return this.appearanceMod();
        }
        if (KeyBinds.CATEGORY.equals(category)) {
            return this.keyBindsMod();
        }
        if (LogsConfig.PAGE.equals(category)) {
            return this.logsMod();
        }
        if (ChangelogConfig.PAGE.equals(category)) {
            return this.changelogMod();
        }
        if (UpdateConfig.CATEGORY.equals(category)) {
            return this.updateMod();
        }
        return null;
    }

    private static int updateRowColor() {
        final int[] palette = Theme.get().systemPalette();
        if (palette.length <= UPDATE_ROW_PALETTE_INDEX) {
            return Theme.get().label();
        }
        return ColorUtils.lerpColor(palette[UPDATE_ROW_PALETTE_INDEX], 0xFF000000, UPDATE_ROW_DARKEN);
    }

    private ModUpdate bannerUpdateFor(final Mod mod) {
        if (mod == null || mod.getContainer() == null || WalksyLibConstants.NAMESPACE.equals(mod.getContainer().getMetadata().getId())) {
            return null;
        }
        final ModUpdate update = WalksyLib.getInstance().getModUpdates().of(mod);
        return update != null && update.isOutdated() ? update : null;
    }

    private Mod updateMod() {
        if (!WalksyLib.getInstance().getModUpdates().library().isOutdated()) {
            return null;
        }
        if (this.updateLibraryMod == null) {
            this.updateLibraryMod = FabricLoader.getInstance()
                .getModContainer(WalksyLibConstants.NAMESPACE)
                .map(container -> Mod.ofDynamicConfig(container, WalksyLib.getInstance().getUpdatePage()::config, UpdateConfig.CATEGORY))
                .orElse(null);
        }
        return this.updateLibraryMod;
    }

    private Mod libraryMod() {
        if (this.library == null) {
            this.library = FabricLoader.getInstance()
                .getModContainer(WalksyLibConstants.NAMESPACE)
                .map(container -> new Mod(container, WalksyLib.getInstance().getGlobalTeams().config(), GlobalTeams.CATEGORY))
                .orElse(null);
        }
        return this.library;
    }

    private Mod appearanceMod() {
        if (this.appearanceLibraryMod == null) {
            this.appearanceLibraryMod = FabricLoader.getInstance()
                .getModContainer(WalksyLibConstants.NAMESPACE)
                .map(container -> new Mod(container, WalksyLib.getInstance().getAppearancePage().config(), AppearanceConfig.CATEGORY))
                .orElse(null);
        }
        return this.appearanceLibraryMod;
    }

    private Mod keyBindsMod() {
        if (this.keyBindsLibraryMod == null) {
            this.keyBindsLibraryMod = FabricLoader.getInstance()
                .getModContainer(WalksyLibConstants.NAMESPACE)
                .map(container -> Mod.ofDynamicConfig(container, WalksyLib.getInstance().getKeyBinds()::config, KeyBinds.CATEGORY))
                .orElse(null);
        }
        return this.keyBindsLibraryMod;
    }

    private Mod logsMod() {
        if (this.logsLibraryMod == null) {
            this.logsLibraryMod = FabricLoader.getInstance()
                .getModContainer(WalksyLibConstants.NAMESPACE)
                .map(container -> Mod.ofDynamicConfig(container, WalksyLib.getInstance().getLogsPage()::config, LogsConfig.PAGE))
                .orElse(null);
        }
        return this.logsLibraryMod;
    }

    private Mod changelogMod() {
        if (this.changelogLibraryMod == null) {
            this.changelogLibraryMod = FabricLoader.getInstance()
                .getModContainer(WalksyLibConstants.NAMESPACE)
                .map(container -> Mod.ofDynamicConfig(container, WalksyLib.getInstance().getChangelogPage()::config, ChangelogConfig.PAGE))
                .orElse(null);
        }
        return this.changelogLibraryMod;
    }

    private Mod devMod() {
        if (!WalksyLibConstants.DEV_BUILD) {
            return null;
        }
        if (this.devPerformanceMod == null) {
            this.devPerformanceMod = FabricLoader.getInstance()
                .getModContainer(WalksyLibConstants.NAMESPACE)
                .map(container -> new Mod(container, WalksyLib.getInstance().getPerformancePage().config(), PerformanceConfig.CATEGORY))
                .orElse(null);
        }
        return this.devPerformanceMod;
    }

    public static void openTeamsEditor() {
        if (active == null) {
            return;
        }
        final Mod library = active.libraryMod();
        if (library != null) {
            active.selectedCategory = null;
            active.selectMod(library);
        }
    }

    public static void openModTeams(final Mod mod) {
        if (active == null || mod == null) {
            return;
        }
        final String category = WalksyLib.getInstance().getGlobalTeams().categoryOf(mod);
        active.selectedCategory = null;
        active.selectMod(mod);
        if (category != null) {
            for (final Category candidate : mod.getConfig().categories()) {
                if (candidate.name().equals(category)) {
                    active.selectCategory(candidate);
                    return;
                }
            }
        }
    }

    private List<Category> fittingCategories(final List<Category> categories, final float budget) {
        final int selected = Math.max(0, categories.indexOf(this.selectedCategory));
        int end = 0;
        while (end < categories.size() && CategoryTabBar.measureWidth(categories.subList(0, end + 1)) <= budget) {
            end++;
        }
        if (selected < end) {
            return categories.subList(0, Math.max(1, end));
        }
        int start = selected;
        while (start > 0 && CategoryTabBar.measureWidth(categories.subList(start - 1, selected + 1)) <= budget) {
            start--;
        }
        return categories.subList(start, selected + 1);
    }

    private List<OptionWidgetFactory.SearchScope> searchScopes() {
        final List<OptionWidgetFactory.SearchScope> scopes = new ArrayList<>();
        for (final Mod other : WalksyLib.getInstance().getModEntryPointList().getMods()) {
            if (!other.hasConfig()) {
                continue;
            }
            final String owner = other.getContainer().getMetadata().getName();
            for (final Category category : other.getConfig().categories()) {
                scopes.add(new OptionWidgetFactory.SearchScope(owner + "  ·  " + category.name(), category));
            }
        }
        this.addLibraryScopes(scopes, this.libraryMod());
        this.addLibraryScopes(scopes, this.appearanceMod());
        this.addLibraryScopes(scopes, this.keyBindsMod());
        this.addLibraryScopes(scopes, this.devMod());
        return scopes;
    }

    private void addLibraryScopes(final List<OptionWidgetFactory.SearchScope> scopes, final Mod library) {
        if (library == null || !library.hasConfig()) {
            return;
        }
        for (final Category category : library.getConfig().categories()) {
            scopes.add(new OptionWidgetFactory.SearchScope("WalksyLib  ·  " + category.name(), category));
        }
    }

    private void selectMod(final Mod mod) {
        if (mod == this.selectedMod && this.selectedCategory != null && !this.showingAllMods) {
            return;
        }
        this.discardPanePopover();
        this.showingAllMods = false;
        this.selectedMod = mod;
        this.updateResetButtonVisibility();
        this.rebuildDetailPane();
    }

    private void updateResetButtonVisibility() {
        if (this.resetButton == null) {
            return;
        }
        final boolean hasConfig = !this.showingAllMods && this.selectedMod != null && this.selectedMod.hasConfig();
        this.resetButton.visible = hasConfig;
        this.resetButton.active = hasConfig && this.hasChangesToReset();
    }

    private boolean hasChangesToReset() {
        if (this.selectedMod == null || !this.selectedMod.hasConfig()) {
            return false;
        }
        return this.selectedMod.getConfig()
            .options()
            .anyMatch(option -> (option.canReset() && option.hasChanged()) || hudMoved(option));
    }

    private static boolean hudMoved(final Option<?> option) {
        final HudSpace space = option.getHudSpace();
        return space != null && !space.isDefault();
    }

    private void askToReset() {
        if (this.resetButton == null || !this.resetButton.active) {
            return;
        }
        this.activePopover.openFaded(this.targetX, this.targetY, this.targetW, this.targetH, this.width, this.height, new ConfirmPopover("Are you sure you want to reset all the settings for " + this.modDisplayName() + " back to default?", "Reset", this::resetSelectedMod, null, this.activePopover::close, null, PopoverHost.PANEL_LAYER));
    }

    private String modDisplayName() {
        return this.selectedMod == null ? "this page" : this.selectedMod.getDisplayName();
    }

    private void resetSelectedMod() {
        if (this.selectedMod == null || !this.selectedMod.hasConfig()) {
            return;
        }
        this.selectedMod.getConfig().options().forEach(option -> {
            option.reset();
            option.runChange();
            if (option.getHudSpace() != null) {
                option.getHudSpace().reset();
            }
        });
        WalksyLib.getInstance().getHudSpaces().save();
        final ScrollPanel previous = this.detailScroll();
        this.rebuildDetailPane();
        final ScrollPanel next = this.detailScroll();
        if (next != null) {
            next.carryScroll(previous);
        }
    }

    private ScrollPanel detailScroll() {
        for (final AbstractWidget widget : this.detailWidgets) {
            if (widget instanceof final ScrollPanel scroll) {
                return scroll;
            }
        }
        return null;
    }

    private void openHudEditor() {
        if (!HudEditScreen.hasAnything()) {
            return;
        }
        this.saveAllModConfigs();
        WLClient.setScreen(new HudEditScreen(this));
    }

    private void showAllMods() {
        if (this.showingAllMods) {
            return;
        }
        this.discardPanePopover();
        this.showingAllMods = true;
        this.selectedMod = null;
        this.selectedCategory = null;
        this.updateResetButtonVisibility();
        this.rebuildDetailPane();
    }

    private void discardPanePopover() {
        this.activePopover.closeInstant();
    }

    private int railMaxHeight() {
        return Math.max(1, Math.round(this.targetH - RAIL_INSET * 2f * WLScreen.uiScaleFactor()));
    }

    private void selectCategory(final Category category) {
        if (category == this.selectedCategory) {
            return;
        }
        this.discardPanePopover();
        this.selectedCategory = category;
        this.rebuildDetailPane();
    }

    private void buildAllModsPane() {
        final int scrollY = this.paneContentTop();
        final int scrollH = Math.max(1, (this.contentY + this.contentH) - scrollY);
        final ScrollPanel scroll = new ScrollPanel(this.contentX, scrollY, this.contentW, scrollH);
        scroll.setTopEdge(scrollY);
        scroll.setContentInsetTop(Math.round(ALL_MODS_CONTENT_TOP_INSET * this.uiScale));
        final List<Mod> mods = WalksyLib.getInstance().getModEntryPointList().getMods();
        if (mods.isEmpty()) {
            scroll.addChild(0, (x, y, w) -> new EmptyStateWidget("No mods yet", "Nothing installed is using WalksyLib", x, y, w, EmptyStateWidget.preferredHeight()));
        } else {
            scroll.addChild(0, (x, y, w) -> new AllModsPaneWidget(mods, this::selectMod, x, y, w));
        }
        scroll.relayout();
        this.addRenderableWidget(scroll);
        this.detailWidgets.add(scroll);
    }

    private int paneContentTop() {
        return this.contentY + Math.round(MOD_TITLE_SIZE * this.uiScale) + Math.round(2 * this.uiScale) + this.subtitleRowHeight();
    }

    private ModMetadata ownMetadata() {
        if (this.showingAllMods || this.selectedMod == null || this.selectedMod.isLibraryPage() || this.selectedMod.getContainer() == null) {
            return null;
        }
        return this.selectedMod.getContainer().getMetadata();
    }

    private String modTitle() {
        return this.showingAllMods ? "All Mods" : this.selectedMod == null ? "" : this.selectedMod.getDisplayName();
    }

    private int titleRowRightLimit() {
        return (this.resetButton == null ? this.contentX + this.contentW : this.resetButton.getX()) - Math.round(AUTHOR_LINK_GAP * this.uiScale);
    }

    private String modAuthor() {
        final String author = this.modAuthorText();
        if (author.isEmpty()) {
            return "";
        }
        final float end = this.contentX + Fonts.SFPRO_DISPLAY_BOLD.width(this.modTitle(), MOD_TITLE_SIZE * this.uiScale) + TITLE_AUTHOR_GAP * this.uiScale + Fonts.SFPRO_DISPLAY.width(author, MOD_AUTHOR_SIZE * this.uiScale);
        return end > this.titleRowRightLimit() ? "" : author;
    }

    private String modAuthorText() {
        final ModMetadata metadata = this.ownMetadata();
        if (metadata == null) {
            return "";
        }
        final List<String> names = metadata.getAuthors()
            .stream()
            .map(Person::getName)
            .filter(name -> name != null && !name.isBlank())
            .toList();
        if (names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return "By " + names.get(0);
        }
        if (names.size() == 2) {
            return "By " + names.get(0) + " and " + names.get(1);
        }
        return "By " + names.get(0) + " and " + (names.size() - 1) + " others";
    }

    private String modLink(final String contactKey) {
        final ModMetadata metadata = this.ownMetadata();
        return metadata == null ? null : metadata.getContact().get(contactKey).orElse(null);
    }

    private float titleBaseline() {
        return this.contentY - Fonts.SFPRO_DISPLAY_BOLD.topForBaseline(0f, MOD_TITLE_SIZE * this.uiScale);
    }

    private float titleRowEndX() {
        float x = this.contentX + Fonts.SFPRO_DISPLAY_BOLD.width(this.modTitle(), MOD_TITLE_SIZE * this.uiScale);
        final String author = this.modAuthor();
        if (!author.isEmpty()) {
            x += TITLE_AUTHOR_GAP * this.uiScale + Fonts.SFPRO_DISPLAY.width(author, MOD_AUTHOR_SIZE * this.uiScale);
        }
        return x;
    }

    private void buildTitleLinks() {
        final int size = Math.round(MOD_TITLE_SIZE * this.uiScale * LINK_BUTTON_RATIO);
        final float capHeight = MOD_TITLE_SIZE * this.uiScale * TITLE_CAP_HEIGHT_RATIO;
        final int y = Math.round(this.titleBaseline() - capHeight / 2f - size / 2f);
        final int limit = this.titleRowRightLimit();
        int x = Math.round(this.titleRowEndX() + AUTHOR_LINK_GAP * this.uiScale);
        for (final String contactKey : new String[]{"sources", "homepage"}) {
            final String url = this.modLink(contactKey);
            if (url == null || url.isBlank() || x + size > limit) {
                continue;
            }
            final LinkButton link = new LinkButton(x, y, size, "sources".equals(contactKey) ? Icons.GITHUB : Icons.WEB, url);
            this.addRenderableWidget(link);
            this.detailWidgets.add(link);
            x += size + Math.round(LINK_GAP * this.uiScale);
        }
    }

    private String modSubtitle() {
        final ModMetadata metadata = this.ownMetadata();
        if (metadata == null) {
            return "";
        }
        final String description = metadata.getDescription();
        return description == null ? "" : description.replaceAll("\\s+", " ").trim();
    }

    private int subtitleRowHeight() {
        return this.modSubtitle().isEmpty() ? 0 : Math.round(MOD_SUBTITLE_SIZE * this.uiScale) + Math.round(TITLE_SUBTITLE_GAP * this.uiScale);
    }

    private void buildSuggestionPane(final ModSuggestion suggestion) {
        final int scrollY = this.paneContentTop();
        final int scrollH = Math.max(1, (this.contentY + this.contentH) - scrollY);
        final ScrollPanel scroll = new ScrollPanel(this.contentX, scrollY, this.contentW, scrollH);
        scroll.setTopEdge(scrollY);
        scroll.addChild(0, (x, y, w) -> new SuggestionPaneWidget(suggestion, x, y, w));
        scroll.relayout();
        this.addRenderableWidget(scroll);
        this.detailWidgets.add(scroll);
    }

    private void buildChangelogPane() {
        WalksyLib.getInstance().getChangelogPage().config();
        final int scrollY = this.paneContentTop();
        final int scrollH = Math.max(1, (this.contentY + this.contentH) - scrollY);
        final ScrollPanel scroll = new ScrollPanel(this.contentX, scrollY, this.contentW, scrollH);
        scroll.setTopEdge(scrollY);
        scroll.setContentInsetTop(Math.round(ALL_MODS_CONTENT_TOP_INSET * this.uiScale));
        scroll.addChild(0, (x, y, w) -> new ChangelogPaneWidget(x, y, w));
        scroll.relayout();
        this.addRenderableWidget(scroll);
        this.detailWidgets.add(scroll);
    }

    private void buildUpdatePane() {
        WalksyLib.getInstance().getChangelogPage().config();
        final ModUpdate update = WalksyLib.getInstance().getModUpdates().library();
        final int scrollY = this.paneContentTop();
        final int scrollH = Math.max(1, (this.contentY + this.contentH) - scrollY);
        final ScrollPanel scroll = new ScrollPanel(this.contentX, scrollY, this.contentW, scrollH);
        scroll.setTopEdge(scrollY);
        scroll.setContentInsetTop(Math.round(ALL_MODS_CONTENT_TOP_INSET * this.uiScale));
        scroll.addChild(0, (x, y, w) -> new UpdateHeroWidget(update, x, y, w));
        scroll.addChild(0, (x, y, w) -> new ChangelogPaneWidget(x, y, w, true));
        scroll.relayout();
        this.addRenderableWidget(scroll);
        this.detailWidgets.add(scroll);
    }

    private void rebuildDetailPane() {
        final CategoryTabBar previousTabBar = this.tabBar;
        for (final AbstractWidget widget : this.detailWidgets) {
            this.removeWidget(widget);
        }
        final CategoryOverflowButton previousOverflow = this.categoryOverflow;
        final CategorySidebarWidget previousRail = this.categoryRail;
        this.detailWidgets.clear();
        this.tabBar = null;
        this.categoryOverflow = null;
        this.categoryRail = null;
        this.builtWithBanner = false;
        CategorySidebarWidget.setWanted(false);
        if (this.contentW <= 0 || this.contentH <= 0) {
            return;
        }
        if (this.showingAllMods) {
            this.buildAllModsPane();
            return;
        }
        if (this.selectedMod == null) {
            return;
        }
        if (this.selectedMod.isSuggestion()) {
            this.buildSuggestionPane(this.selectedMod.getSuggestion());
            return;
        }
        if (this.selectedMod == this.changelogLibraryMod && this.searchQuery.isBlank()) {
            this.buildChangelogPane();
            return;
        }
        if (this.selectedMod == this.updateLibraryMod && this.searchQuery.isBlank()) {
            this.buildUpdatePane();
            return;
        }
        if (!this.selectedMod.hasConfig()) {
            return;
        }
        final float scale = this.uiScale;
        final Mod mod = this.selectedMod;
        final List<Category> categories = mod.getConfig().categories();
        if (this.selectedCategory == null || !categories.contains(this.selectedCategory)) {
            this.selectedCategory = categories.isEmpty() ? null : categories.get(0);
        }
        this.buildTitleLinks();
        final int titleRowHeight = Math.round(MOD_TITLE_SIZE * scale) + Math.round(6 * scale) + this.subtitleRowHeight();
        final int titleTabsGap = Math.round(TITLE_TABS_GAP * scale);
        final int tabHeight = Math.round(TAB_HEIGHT * scale);
        final int scrollGap = Math.round(SAVE_ROW_GAP * scale);
        final int tabsRowY = this.contentY + titleRowHeight + titleTabsGap;
        final boolean searching = !this.searchQuery.isBlank();
        final boolean manyCategories = !searching && categories.size() > 1;
        final boolean railMode = WalksyLib.getInstance().getAppearancePage().sidebarCategories();
        final boolean showTabs = manyCategories && !railMode;
        final boolean railWanted = manyCategories && railMode;
        CategorySidebarWidget.setWanted(railWanted);
        if (this.firstLayout) {
            this.firstLayout = false;
            CategorySidebarWidget.snapToWanted();
        }
        if (railWanted || !CategorySidebarWidget.isGone()) {
            final float railCenterY = this.targetY + this.targetH / 2f;
            final int railMaxHeight = this.railMaxHeight();
            List<Category> railCategories = railWanted ? categories : this.lastRailCategories;
            if (railWanted) {
                this.lastRailCategories = categories;
            }
            if (railCategories.isEmpty()) {
                railCategories = categories;
            }
            final CategorySidebarWidget rail = previousRail != null && previousRail.matches(railCategories, this.targetX + this.targetW, railCenterY, railMaxHeight) && !previousRail.isLeaving() ? previousRail : new CategorySidebarWidget(railCategories, this::selectCategory, () -> this.selectedCategory, this.targetX + this.targetW, railCenterY, railMaxHeight);
            this.addRenderableWidget(rail);
            this.detailWidgets.add(rail);
            this.categoryRail = rail;
        }
        if (showTabs) {
            List<Category> visible = categories;
            final boolean overflowing = CategoryTabBar.measureWidth(categories) > this.contentW;
            if (overflowing) {
                visible = this.fittingCategories(categories, this.contentW - tabHeight - Math.round(OVERFLOW_GAP * scale));
            }
            final List<Category> shown = visible;
            final CategoryTabBar tabBar = previousTabBar != null && previousTabBar.matches(shown, this.contentX, tabsRowY, tabHeight) ? previousTabBar : new CategoryTabBar(shown, this::selectCategory, () -> this.selectedCategory, this.activePopover, this.contentX, tabsRowY, tabHeight);
            this.addRenderableWidget(tabBar);
            this.detailWidgets.add(tabBar);
            this.tabBar = tabBar;
            if (overflowing) {
                final int buttonX = this.contentX + this.contentW - tabHeight;
                final int overflowMaxHeight = this.railMaxHeight();
                final float overflowCenterY = this.targetY + this.targetH / 2f;
                final CategoryOverflowButton overflow = previousOverflow != null && previousOverflow.matches(categories, overflowMaxHeight, overflowCenterY, buttonX, tabsRowY, tabHeight) ? previousOverflow : new CategoryOverflowButton(categories, this::selectCategory, () -> this.selectedCategory, (anchor, ax, ay, content) -> this.activePopover.openAt(anchor, ax, ay, this.width, this.height, content), this.activePopover::close, this.targetX + this.targetW, overflowMaxHeight, overflowCenterY, buttonX, tabsRowY, tabHeight);
                this.addRenderableWidget(overflow);
                this.detailWidgets.add(overflow);
                this.categoryOverflow = overflow;
            }
        }
        int scrollY = showTabs ? tabsRowY + tabHeight + scrollGap : tabsRowY;
        final ModUpdate modUpdate = this.bannerUpdateFor(mod);
        final boolean bannerShown = modUpdate != null;
        this.builtWithBanner = bannerShown;
        if (bannerShown) {
            final int bannerH = Math.round(ModUpdateBanner.preferredHeight(scale));
            final ModUpdateBanner banner = new ModUpdateBanner(modUpdate, this.contentX, scrollY, this.contentW);
            this.addRenderableWidget(banner);
            this.detailWidgets.add(banner);
            scrollY += bannerH + scrollGap;
        }
        final int scrollH = Math.max(1, (this.contentY + this.contentH) - scrollY);
        if (!searching && this.selectedCategory == null) {
            return;
        }
        final ScrollPanel scroll = new ScrollPanel(this.contentX, scrollY, this.contentW, scrollH);
        final int edgeTop;
        if (bannerShown) {
            edgeTop = scrollY;
        } else if (showTabs) {
            edgeTop = tabsRowY - Math.round(TAB_EDGE_LIFT * this.uiScale);
        } else {
            edgeTop = tabsRowY - titleTabsGap;
        }
        scroll.setTopEdge(edgeTop);
        scroll.setScrollbarInsets(CONTENT_SCROLLBAR_END_INSET, 0f);
        final RowContext ctx = new RowContext(option -> { }, (anchor, content) -> this.activePopover.open(anchor, this.width, this.height, content), (anchor, content) -> this.activePopover.openCentered(anchor, this.targetX, this.targetY, this.targetW, this.targetH, this.width, this.height, content), (anchor, x, y, content) -> this.activePopover.openAt(anchor, x, y, this.width, this.height, content), this.activePopover::close);
        if (searching) {
            final String query = this.searchQuery.trim();
            OptionWidgetFactory.populateSearchScopes(scroll, this.searchScopes(), this.searchQuery, ctx, (x, y, w) -> new EmptyStateWidget("No options found", "Nothing in this window matches \"" + query + "\"", x, y, w, EmptyStateWidget.preferredHeight()));
        } else {
            final boolean keyBinds = KeyBinds.CATEGORY.equals(this.selectedCategory.name());
            OptionWidgetFactory.populate(scroll, this.selectedCategory, ctx, (x, y, w) -> new EmptyStateWidget(keyBinds ? "No key binds" : "Nothing here", keyBinds ? "No mod has registered a key bind yet" : "This category has no options", x, y, w, EmptyStateWidget.preferredHeight()));
        }
        this.addRenderableWidget(scroll);
        this.detailWidgets.add(scroll);
    }

    private void applySearch(final String query) {
        final String next = query == null ? "" : query;
        if (next.equals(this.searchQuery)) {
            return;
        }
        this.searchQuery = next;
        this.activePopover.close();
        this.rebuildDetailPane();
    }

    @Override
    protected void applyContentTransform(final Matrix3x2fStack pose) {
        final LiquidRect cur = this.transition.currentPrimary();
        if (cur == null || this.targetW <= 0 || this.targetH <= 0 || this.contentAtRest(cur)) {
            return;
        }
        final float restCx = this.targetX + this.targetW / 2f;
        final float restCy = this.targetY + this.targetH / 2f;
        final float sx = cur.w() / this.targetW;
        final float sy = cur.h() / this.targetH;
        pose.translate(cur.centerX(), cur.centerY());
        pose.scale(sx, sy);
        pose.translate(-restCx, -restCy);
    }

    @Override
    protected void prepareWarmFrame() {
        this.warming = true;
    }

    private void advanceTransition() {
        final long frame = WLRenderSystem.frameId();
        if (this.warming || this.animatedFrame == frame) {
            return;
        }
        this.animatedFrame = frame;
        this.transition.update(WLRenderSystem.frameDeltaTicks());
        this.advanceAppearanceSwap(WLRenderSystem.frameDeltaTicks());
    }

    @Override
    protected boolean isAnimating() {
        return !this.contentAtRest(this.transition.currentPrimary()) || this.appearanceSwap < 1f;
    }

    private boolean contentAtRest(@Nullable final LiquidRect cur) {
        if (this.transition.isSettled() || cur == null) {
            return true;
        }
        final float tolerance = REST_TOLERANCE_PX / DevicePixels.perLogicalX();
        return Math.abs(cur.x() - this.targetX) <= tolerance && Math.abs(cur.y() - this.targetY) <= tolerance && Math.abs(cur.w() - this.targetW) <= tolerance && Math.abs(cur.h() - this.targetH) <= tolerance;
    }

    @Override
    protected float contentOpacity() {
        return Mth.clamp(this.transition.value(), 0f, 1f) * this.swapContentAlpha();
    }

    @Override
    protected void extractBlurredBackground(final WLGraphics2D graphics) {
        this.advanceTransition();
        if (this.tabBar != null) {
            final Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            if (!this.isContentTransformActive()) {
                this.applyContentTransform(pose);
            }
            this.tabBar.extractEarlyText(graphics);
            pose.popMatrix();
        }
        WLRenderSystem.glassUniforms().requestGlassPass(graphics);
    }

    @Override
    public void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        this.advanceTransition();
        CategorySidebarWidget.tickPresence(WLRenderSystem.frameDeltaSeconds());
        final GlassStyle style = this.swapBlend(this.swapPanelFrom, this.panelShellStyle()).copy().opacity(this.transition.value());
        this.transition.emit(graphics, style, PANEL_LAYER, this.shellMinecraftAmount());
        this.extractSidebar(graphics);
        ResetButton.lift(false, () -> this.extractModTitle(graphics));
        this.activePopover.setHostAlpha(this.swapContentAlpha());
        this.activePopover.extract(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void extractOverlay(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        if (WalksyLibConstants.DEV_BUILD) {
            PerformanceMonitorWindow.get().extract(graphics, this.width, this.height, mouseX, mouseY);
        }
    }

    private void extractModTitle(final WLGraphics2D graphics) {
        if (this.selectedMod == null && !this.showingAllMods) {
            return;
        }
        final float op = this.contentOpacity();
        if (op <= 0.001f) {
            return;
        }
        final float size = MOD_TITLE_SIZE * this.uiScale;
        final String name = this.showingAllMods ? "All Mods" : this.selectedMod.getDisplayName();
        final Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        this.applyContentTransform(pose);
        Fonts.SFPRO_DISPLAY_BOLD.extract(graphics, name, this.contentX, this.contentY, size, ColorUtils.withAlpha(Theme.get().label(), op));
        final String author = this.modAuthor();
        if (!author.isEmpty()) {
            final float authorSize = MOD_AUTHOR_SIZE * this.uiScale;
            Fonts.SFPRO_DISPLAY.extract(graphics, author, this.contentX + Fonts.SFPRO_DISPLAY_BOLD.width(name, size) + TITLE_AUTHOR_GAP * this.uiScale, Fonts.SFPRO_DISPLAY.topForBaseline(this.titleBaseline(), authorSize), authorSize, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), op));
        }
        final String subtitle = this.modSubtitle();
        if (!subtitle.isEmpty()) {
            final float subtitleSize = MOD_SUBTITLE_SIZE * this.uiScale;
            Fonts.SFPRO_DISPLAY.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(subtitle, subtitleSize, this.contentW), this.contentX, this.contentY + size + TITLE_SUBTITLE_GAP * this.uiScale, subtitleSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), op));
        }
        pose.popMatrix();
    }

    private void extractSidebar(final WLGraphics2D graphics) {
        if (this.targetW <= 0 || this.targetH <= 0) {
            return;
        }
        final LiquidRect r = this.toContentRect(this.sidebarX, this.sidebarY, this.sidebarW, this.sidebarH, this.sidebarCornerRadius);
        final float op = Mth.clamp(this.transition.value(), 0f, 1f);
        if (op <= 0.001f) {
            return;
        }
        final GlassStyle shell = this.swapBlend(this.swapSidebarFrom, this.sidebarShellStyle());
        final GlassStyle style = shell.copy().opacity(shell.getOpacity() * op);
        graphics.glass().dimensions(r.x(), r.y(), r.w(), r.h()).cornerRadius(r.cornerRadius()).style(style).minecraftAmount(this.shellMinecraftAmount()).extract();
    }

    private LiquidRect toContentRect(final float x, final float y, final float w, final float h, final float radius) {
        final LiquidRect cur = this.transition.currentPrimary();
        if (cur == null) {
            return new LiquidRect(x, y, w, h, radius);
        }
        final float restCx = this.targetX + this.targetW / 2f;
        final float restCy = this.targetY + this.targetH / 2f;
        final float sx = cur.w() / this.targetW;
        final float sy = cur.h() / this.targetH;
        final float nx = cur.centerX() + sx * (x - restCx);
        final float ny = cur.centerY() + sy * (y - restCy);
        return new LiquidRect(nx, ny, w * sx, h * sy, radius * Math.min(sx, sy));
    }

    @Override
    protected void extractTransparentBackground(final WLGraphics2D graphics) {
    }

    @Override
    protected void extractMenuBackground(final WLGraphics2D graphics, final int x, final int y, final int width, final int height) {
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent click, final boolean doubled) {
        if (WalksyLibConstants.DEV_BUILD && PerformanceMonitorWindow.get().mouseClicked(click.x(), click.y())) {
            this.pressTarget = PressTarget.NONE;
            return true;
        }
        if (this.activePopover.isOpen() && this.tabBar != null && this.tabBar.isMouseOver(click.x(), click.y()) && !this.activePopover.isOverPanel(click.x(), click.y())) {
            this.activePopover.closeInstant();
        }
        if (this.activePopover.isOpen() && this.activePopover.mouseClicked(click, doubled)) {
            this.pressTarget = PressTarget.POPOVER;
            return true;
        }
        this.pressTarget = PressTarget.WIDGETS;
        final boolean handled = super.mouseClicked(click, doubled);
        if (this.searchBar != null && this.searchBar.isFocused() && !this.searchBar.isMouseOver(click.x(), click.y())) {
            this.setFocused(null);
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (WalksyLibConstants.DEV_BUILD && PerformanceMonitorWindow.get().mouseDragged(event.x(), event.y())) {
            return true;
        }
        if (this.pressTarget == PressTarget.POPOVER) {
            return this.activePopover.mouseDragged(event, dragX, dragY);
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (WalksyLibConstants.DEV_BUILD && PerformanceMonitorWindow.get().mouseReleased()) {
            this.pressTarget = PressTarget.NONE;
            return true;
        }
        final PressTarget target = this.pressTarget;
        this.pressTarget = PressTarget.NONE;
        if (target == PressTarget.POPOVER) {
            return this.activePopover.mouseReleased(event);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final KeybindOptionWidget capturing = KeybindOptionWidget.capturing();
        if (capturing != null) {
            return capturing.keyPressed(event);
        }
        if (this.activePopover.isOpen()) {
            return this.activePopover.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (this.activePopover.isOpen()) {
            return this.activePopover.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (WalksyLibConstants.DEV_BUILD && PerformanceMonitorWindow.get().contains(mouseX, mouseY)) {
            return true;
        }
        if (this.activePopover.isOpen()) {
            this.activePopover.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.pendingSwap == null && this.pendingAppearanceRebuild >= 0 && !WalksyLib.getInstance().getMouseHandler().isLeftDown() && this.pendingAppearanceRebuild-- == 0) {
            this.beginSwap(() -> {}, false);
        }
        if (this.editHudRow != null) {
            this.editHudRow.active = HudEditScreen.hasAnything();
        }
        if (this.pendingSwap == null) {
            if (WalksyLib.getInstance().getModUpdates().library().isOutdated() != this.builtWithUpdateRow) {
                rebuildWidgets();
            } else if ((this.bannerUpdateFor(this.selectedMod) != null) != this.builtWithBanner) {
                this.rebuildDetailPane();
            } else if (this.selectedMod != null && (this.selectedMod == this.changelogLibraryMod || this.selectedMod == this.updateLibraryMod) && !this.showingAllMods && WalksyLib.getInstance().getChangelogPage().isStale()) {
                this.rebuildDetailPane();
            }
        }
        this.updateResetButtonVisibility();
        if (this.pendingSwap != null) {
            if (this.appearanceSwap >= APPEARANCE_SWAP_FLIP && (!this.swapClosesPopover || !this.activePopover.isOpen())) {
                this.finishSwap();
            }
        } else if (this.transition.isDoneClosing()) {
            super.onClose();
        }
    }

    @Override
    public void onClose() {
        KeybindOptionWidget.cancelCapture();
        this.applyPendingSwap();
        this.appearanceSwap = 1f;
        if (this.transition.isClosing()) {
            super.onClose();
            return;
        }
        this.saveAllModConfigs();
        if (UiStyle.minecraft()) {
            super.onClose();
            return;
        }
        final float sw = this.targetW * 0.9f;
        final float sh = this.targetH * 0.9f;
        final LiquidRect shrunken = new LiquidRect(this.targetX + (this.targetW - sw) / 2f, this.targetY + (this.targetH - sh) / 2f, sw, sh, this.panelCornerRadius);
        this.transition.setEndpoints(shrunken, this.getBounds());
        this.transition.profile().wobble = 0f;
        this.transition.profile().easing = ScreenAnimation.Easing.EASE_OUT_CUBIC;
        this.transition.closeSymmetric();
    }

    private void saveAllModConfigs() {
        for (final Mod mod : WalksyLib.getInstance().getModEntryPointList().getMods()) {
            if (mod.hasConfig()) {
                mod.getConfig().saveAndNotify();
            }
        }
        WalksyLib.getInstance().getGlobalTeams().save();
        WalksyLib.getInstance().getAppearancePage().save();
        WalksyLib.getInstance().getKeyBinds().save();
        SpriteCache.sweepUnused();
    }

    private LiquidRect getBounds() {
        return new LiquidRect(this.targetX, this.targetY, this.targetW, this.targetH, this.panelCornerRadius);
    }

    private enum PressTarget {
        NONE,
        POPOVER,
        WIDGETS
    }
}
