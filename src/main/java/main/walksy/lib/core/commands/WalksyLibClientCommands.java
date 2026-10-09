package main.walksy.lib.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import main.walksy.lib.core.WalksyLib;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class WalksyLibClientCommands {
    private static final List<Registration> REGISTRY = new ArrayList<>();
    private static final Set<String> CLAIMED = new LinkedHashSet<>();
    private static boolean listenerRegistered;

    private WalksyLibClientCommands() {
    }

    public static void register(final String name, final Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> body) {
        register(name, List.of(), body);
    }

    public static void register(final String name, final List<String> aliases, final Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> body) {
        REGISTRY.add(new Registration(name, List.copyOf(aliases), body));
        claim(name);
        for (final String alias : aliases) {
            claim(alias);
        }
        ensureListener();
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private static void claim(final String name) {
        if (!CLAIMED.add(name)) {
            WalksyLib.getLogger().err("Client command /" + name + " is registered twice (the two trees will be merged)");
        }
    }

    private static void ensureListener() {
        if (listenerRegistered) {
            return;
        }
        listenerRegistered = true;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (final Registration registration : REGISTRY) {
                registration.addTo(dispatcher);
            }
        });
    }

    private record Registration(String name, List<String> aliases, Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> body) {
        private void addTo(final CommandDispatcher<FabricClientCommandSource> dispatcher) {
            final LiteralArgumentBuilder<FabricClientCommandSource> root = literal(this.name);
            this.body.accept(root);
            final LiteralCommandNode<FabricClientCommandSource> node = dispatcher.register(root);
            for (final String alias : this.aliases) {
                final LiteralArgumentBuilder<FabricClientCommandSource> redirect = literal(alias).redirect(node);
                if (node.getCommand() != null) {
                    redirect.executes(node.getCommand());
                }
                dispatcher.register(redirect);
            }
        }
    }
}
