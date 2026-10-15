package main.walksy.lib.core.platform.compat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibEntrypoint;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.platform.compat.command.ClientCommandBridge;
import main.walksy.lib.core.platform.compat.networking.ClientPlayNetworking;
import main.walksy.lib.core.platform.compat.networking.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NeoForgeBootstrap {
    private static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
    private static final List<Deferred> DEFERRED = new ArrayList<>();
    private static boolean installed;
    private static boolean started;
    private static boolean keyMappingsClosed;

    private NeoForgeBootstrap() {
    }

    public static void defer(final String modId, final Runnable main, final Runnable client) {
        synchronized (DEFERRED) {
            DEFERRED.add(new Deferred(modId, main, client));
        }
    }

    private record Deferred(String modId, Runnable main, Runnable client) {
    }

    public static synchronized void install(final IEventBus modBus) {
        if (installed) {
            return;
        }
        installed = true;
        modBus.addListener(EventPriority.HIGHEST, NeoForgeBootstrap::onRegister);
        modBus.addListener(EventPriority.HIGHEST, NeoForgeBootstrap::onRegisterKeyMappings);
        modBus.addListener(EventPriority.HIGHEST, NeoForgeBootstrap::onRegisterPayloadHandlers);
        modBus.addListener(NeoForgeBootstrap::onClientSetup);
        NeoForge.EVENT_BUS.addListener(NeoForgeBootstrap::onRegisterClientCommands);
    }

    public static void addKeyMapping(final KeyMapping mapping) {
        synchronized (KEY_MAPPINGS) {
            if (keyMappingsClosed) {
                WalksyLib.getLogger().err("Key mapping " + mapping.getName() + " was created after NeoForge finished collecting key mappings, so it will not be bindable in the vanilla controls screen");
                return;
            }
            KEY_MAPPINGS.add(mapping);
        }
    }

    private static synchronized void ensureStarted() {
        if (started) {
            return;
        }
        started = true;
        final List<Deferred> ordered;
        synchronized (DEFERRED) {
            ordered = new ArrayList<>(DEFERRED);
        }
        ordered.sort(Comparator.comparing(Deferred::modId));

        for (final Deferred deferred : ordered) {
            if (deferred.main() != null) {
                deferred.main().run();
            }
        }
        new WalksyLibEntrypoint().onInitializeClient();
        for (final Deferred deferred : ordered) {
            if (deferred.client() != null) {
                deferred.client().run();
            }
        }
    }

    private static void onRegister(final RegisterEvent event) {
        ensureStarted();
    }

    private static void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        ensureStarted();
        synchronized (KEY_MAPPINGS) {
            for (final KeyMapping mapping : KEY_MAPPINGS) {
                event.register(mapping);
            }
            keyMappingsClosed = true;
        }
    }

    private static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        ensureStarted();
        final PayloadRegistrar registrar = event.registrar("1").optional();
        for (final PayloadTypeRegistry.Registration<?> registration : PayloadTypeRegistry.registrations()) {
            registerPayload(registrar, registration);
        }
    }

    private static <T extends CustomPacketPayload> void registerPayload(final PayloadRegistrar registrar, final PayloadTypeRegistry.Registration<T> registration) {
        if (registration.clientbound()) {
            registrar.playToClient(registration.type(), registration.codec(), (payload, context) -> ClientPlayNetworking.dispatch(payload));
        } else {
            registrar.playToServer(registration.type(), registration.codec(), (payload, context) -> {
            });
        }
    }

    private static void onRegisterClientCommands(final RegisterClientCommandsEvent event) {
        ensureStarted();
        ClientCommandBridge.bridge(event.getDispatcher(), event.getBuildContext());
    }

    private static void auditMixinsIfRequested() {
        if (Boolean.getBoolean("walksylib.auditMixins")) {
            MixinEnvironment.getCurrentEnvironment().audit();
            auditCommandBridge();
        }
    }

    private static void auditCommandBridge() {
        final CommandDispatcher<CommandSourceStack> scratch = new CommandDispatcher<>();
        ClientCommandBridge.bridge(scratch, null);
        final StringBuilder roots = new StringBuilder();
        for (final CommandNode<CommandSourceStack> root : scratch.getRoot().getChildren()) {
            scratch.parse(root.getName(), null);
            roots.append('/').append(root.getName()).append(' ');
        }
        WalksyLib.getLogger().info("Client command bridge ok: " + roots.toString().trim());
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        ensureStarted();
        event.enqueueWork(() -> {
            auditMixinsIfRequested();
            ModList.get().getModContainerById("walksylib").ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new WalksyLibPanelScreen(parent, "WalksyLib").openOnAllMods()));
            for (final Mod mod : WalksyLib.getInstance().getModEntryPointList().getMods()) {
                if (mod.getContainer() == null || !mod.hasConfig()) {
                    continue;
                }
                final String id = mod.getContainer().getMetadata().getId();
                final String name = mod.getContainer().getMetadata().getName();
                ModList.get().getModContainerById(id).ifPresent(container ->
                    container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new WalksyLibPanelScreen(parent, name).preselect(id)));
            }
        });
    }
}
