package main.walksy.lib.core;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Holder;
import main.walksy.lib.core.sound.SoundAttributors;
import main.walksy.lib.core.input.WLMouseHandler;
import main.walksy.lib.core.config.internal.AppearanceConfig;
import main.walksy.lib.core.commands.WalksyLibCommands;
import main.walksy.lib.core.config.TickableOptions;
import main.walksy.lib.core.config.internal.PerformanceConfig;
import main.walksy.lib.core.hud.HudSpaces;
import main.walksy.lib.core.keybinds.KeyBinds;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.mods.ModEntryPointList;
import main.walksy.lib.core.mods.data.ShieldStateDataTracker;
import main.walksy.lib.core.mods.remote.ModUpdate;
import main.walksy.lib.core.mods.remote.Changelog;
import main.walksy.lib.core.mods.remote.ModUpdates;
import main.walksy.lib.core.mods.remote.SupersededJars;
import main.walksy.lib.core.network.ServerControlNetworking;
import main.walksy.lib.core.network.ServerOptOuts;
import main.walksy.lib.api.config.value.WLKeyMapping;
import main.walksy.lib.core.gui.widget.options.types.CroppedTextureOptionWidget;
import main.walksy.lib.core.gui.widget.options.types.TextureOptionWidget;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.teams.GlobalTeams;
import main.walksy.lib.core.toast.WLToast;
import main.walksy.lib.core.utils.log.WLLogger;
import main.walksy.lib.core.toast.WLToastManager;
import main.walksy.lib.api.config.option.OptionChangeLogger;
import main.walksy.lib.api.config.value.OfflineRegistries;
import main.walksy.lib.core.config.serialization.FavoriteColors;
import main.walksy.lib.api.render.PreviewEntities;
import main.walksy.lib.core.render.perf.PanelWarmup;
import main.walksy.lib.core.mods.remote.SuggestedMods;
import main.walksy.lib.core.config.internal.ChangelogConfig;
import main.walksy.lib.core.config.internal.LogsConfig;
import main.walksy.lib.core.config.internal.UpdateConfig;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public final class WalksyLib {
    private static final WLLogger LOGGER = new WLLogger();
    private static final float LAUNCH_NOTICE_SECONDS = 10f;
    private static final float INSTALL_NOTICE_SECONDS = 12f;
    private static volatile WalksyLib instance;
    private final SoundAttributors soundAttributors;
    private final OptionChangeLogger optionChangeLogger;
    private final OfflineRegistries offlineRegistries;
    private final FavoriteColors favoriteColors;
    private final PreviewEntities previewEntities;
    private final PanelWarmup panelWarmup;
    private final Changelog changelog;
    private final SuggestedMods suggestedMods;
    private final ChangelogConfig changelogPage;
    private final LogsConfig logsPage;
    private final UpdateConfig updatePage;
    private final ShieldStateDataTracker shieldStateData;
    private final ModEntryPointList modEntryPointList;
    private final TickableOptions tickableOptions;
    private final WLToastManager toastManager;
    private final GlobalTeams globalTeams;
    private final AppearanceConfig appearancePage;
    private final PerformanceConfig performancePage;
    private final KeyBinds keyBinds;
    private final HudSpaces hudSpaces;
    private final ModUpdates modUpdates;
    private final ServerOptOuts serverOptOuts;
    private final ServerControlNetworking serverControl;
    private final WLMouseHandler mouseHandler;

    private WalksyLib() {
        instance = this;
        this.soundAttributors = new SoundAttributors();
        this.optionChangeLogger = new OptionChangeLogger();
        this.offlineRegistries = new OfflineRegistries();
        this.favoriteColors = new FavoriteColors();
        this.previewEntities = new PreviewEntities();
        this.panelWarmup = new PanelWarmup();
        this.changelog = new Changelog();
        this.suggestedMods = new SuggestedMods();
        this.changelogPage = new ChangelogConfig();
        this.logsPage = new LogsConfig();
        this.updatePage = new UpdateConfig();
        this.shieldStateData = new ShieldStateDataTracker();
        this.modEntryPointList = new ModEntryPointList();
        this.tickableOptions = new TickableOptions();
        this.toastManager = new WLToastManager();
        this.globalTeams = new GlobalTeams();
        this.appearancePage = new AppearanceConfig();
        this.performancePage = new PerformanceConfig();
        this.keyBinds = new KeyBinds();
        this.hudSpaces = new HudSpaces();
        this.modUpdates = new ModUpdates();
        this.serverOptOuts = new ServerOptOuts();
        this.serverControl = new ServerControlNetworking(this.serverOptOuts);
        this.mouseHandler = new WLMouseHandler();
    }

    public void onInitializeClient() {
        this.loadConfigs();
        this.registerHudSpaces();
        this.tickableOptions.collect(this.modEntryPointList);
        this.tickableOptions.add(this.appearancePage::config);
        this.registerServerControl();
        this.registerUpdateChecks();
    }

    public void onClientReady() {
        Fonts.load();
        Fonts.warm(WLScreen.uiScaleFactor());
    }

    public void onClientTickEnd(final Minecraft minecraft) {
        this.tickableOptions.tick();
        this.serverControl.tick();
    }

    public void onClientStopping() {
        this.globalTeams.save();
        this.appearancePage.save();
        this.keyBinds.save();
        this.hudSpaces.save();
        this.modEntryPointList.saveModConfigs();
    }

    public void onKeyInput(final int code, final int action, final int scancode) {
        WLKeyMapping.handleLocalKey(code, action, scancode);
    }

    public void onFileDrop(final List<Path> files) {
        TextureOptionWidget.handleFileDrop(files);
        CroppedTextureOptionWidget.handleFileDrop(files);
    }

    public void onJoinedServer() {
        this.serverControl.onJoinedServer();
    }

    public void onLeftServer() {
        this.serverControl.onLeftServer();
        this.shieldStateData.clear();
        this.soundAttributors.clear();
    }

    public void onSoundEvent(final Holder<SoundEvent> sound, final double x, final double y, final double z) {
        if (Minecraft.getInstance().isSameThread()) {
            this.soundAttributors.onSound(sound, x, y, z);
        }
    }

    public void onLocalPlayerTick() {
        this.soundAttributors.tick();
        this.shieldStateData.tick();
    }

    private void loadConfigs() {
        WLKeyMapping.attachPending();
        this.modEntryPointList.retrieve();
        this.globalTeams.config();
        this.appearancePage.config();
        if (WalksyLibConstants.DEV_BUILD) {
            this.performancePage.config();
        }
        this.modEntryPointList.loadModConfigs();
        this.keyBinds.config();
        WalksyLibCommands.register();
    }

    private void registerHudSpaces() {
        for (final Mod mod : this.modEntryPointList.getMods()) {
            final ModContainer container = mod.getContainer();
            if (mod.hasConfig() && container != null) {
                this.hudSpaces.registerFrom(container.getMetadata().getId(), mod.getConfig());
            }
        }
        this.hudSpaces.warnUnregistered();
    }

    private void registerServerControl() {
        for (final Mod mod : this.modEntryPointList.getMods()) {
            this.serverOptOuts.register(mod);
        }
        this.serverControl.register();
    }

    private void registerUpdateChecks() {
        this.modUpdates.library();
        for (final Mod mod : this.modEntryPointList.getMods()) {
            final ModContainer container = mod.getContainer();
            if (container != null && (!mod.hasConfig() || mod.getConfig().checksForUpdates())) {
                this.modUpdates.registerIfPublished(container.getMetadata().getId());
            }
        }
        SupersededJars.sweepAsync(this.modUpdates.trackedIds());
        this.modUpdates.onInstallFinished(this::notifyInstalled);
        this.modUpdates.checkAll(this::notifyOutdated);
        WalksyLib.getInstance().getChangelog().ensureLoaded();
    }

    private void notifyInstalled(final ModUpdate update) {
        if (update.installState() == ModUpdate.Install.FAILED) {
            this.toastManager.submit(WLToast.of(update.displayName() + " could not be updated", update.installError().isBlank() ? "The download or the file swap did not go through." : update.installError())
                .icon(Icons.UPDATE)
                .id("walksylib:update-install:" + update.modId())
                .seconds(INSTALL_NOTICE_SECONDS)
                .error());
            return;
        }
        if (update.installState() != ModUpdate.Install.DONE) {
            return;
        }
        this.toastManager.submit(WLToast.of("Restart to finish updating " + update.displayName(), update.displayName() + " " + update.latestVersion() + " is ready and loads the next time you launch the game.")
            .icon(Icons.UPDATE)
            .id("walksylib:update-install:" + update.modId())
            .seconds(INSTALL_NOTICE_SECONDS)
            .success());
    }

    private void notifyOutdated(final ModUpdate update) {
        this.toastManager.submit(WLToast.of(update.displayName() + " " + update.latestVersion() + " is out", "You have " + update.currentVersion() + ". Open the WalksyLib panel to install it.")
            .icon(Icons.UPDATE)
            .id("walksylib:update:" + update.modId())
            .seconds(LAUNCH_NOTICE_SECONDS)
            .info());
    }

    public static WLLogger getLogger() {
        return LOGGER;
    }

    public static WalksyLib getInstance() {
        final WalksyLib existing = instance;
        if (existing != null) {
            return existing;
        }
        synchronized (WalksyLib.class) {
            if (instance == null) {
                new WalksyLib();
            }
            return instance;
        }
    }

    public static @Nullable WalksyLib getInstanceOrNull() {
        return instance;
    }

    public SoundAttributors getSoundAttributors() {
        return this.soundAttributors;
    }

    public OptionChangeLogger getOptionChangeLogger() {
        return this.optionChangeLogger;
    }

    public OfflineRegistries getOfflineRegistries() {
        return this.offlineRegistries;
    }

    public FavoriteColors getFavoriteColors() {
        return this.favoriteColors;
    }

    public PreviewEntities getPreviewEntities() {
        return this.previewEntities;
    }

    public PanelWarmup getPanelWarmup() {
        return this.panelWarmup;
    }

    public Changelog getChangelog() {
        return this.changelog;
    }

    public SuggestedMods getSuggestedMods() {
        return this.suggestedMods;
    }

    public ChangelogConfig getChangelogPage() {
        return this.changelogPage;
    }

    public LogsConfig getLogsPage() {
        return this.logsPage;
    }

    public UpdateConfig getUpdatePage() {
        return this.updatePage;
    }

    public WLToastManager getToastManager() {
        return this.toastManager;
    }

    public WLMouseHandler getMouseHandler() {
        return this.mouseHandler;
    }

    public ShieldStateDataTracker getShieldStateData() {
        return this.shieldStateData;
    }

    public ModEntryPointList getModEntryPointList() {
        return this.modEntryPointList;
    }

    public GlobalTeams getGlobalTeams() {
        return this.globalTeams;
    }

    public AppearanceConfig getAppearancePage() {
        return this.appearancePage;
    }

    public PerformanceConfig getPerformancePage() {
        return this.performancePage;
    }

    public KeyBinds getKeyBinds() {
        return this.keyBinds;
    }

    public HudSpaces getHudSpaces() {
        return this.hudSpaces;
    }

    public ModUpdates getModUpdates() {
        return this.modUpdates;
    }

    public ServerOptOuts getServerOptOuts() {
        return this.serverOptOuts;
    }
}
