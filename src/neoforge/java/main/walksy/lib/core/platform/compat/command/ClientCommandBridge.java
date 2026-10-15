package main.walksy.lib.core.platform.compat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ClientCommandBridge {
    private ClientCommandBridge() {
    }

    public static void bridge(final CommandDispatcher<CommandSourceStack> target, final CommandBuildContext buildContext) {
        final CommandDispatcher<FabricClientCommandSource> shim = new CommandDispatcher<>();
        ClientCommandRegistrationCallback.EVENT.invoker().register(shim, buildContext);
        final Map<CommandNode<FabricClientCommandSource>, CommandNode<CommandSourceStack>> converted = new IdentityHashMap<>();
        for (final CommandNode<FabricClientCommandSource> root : shim.getRoot().getChildren()) {
            target.getRoot().addChild(convert(root, converted));
        }
    }

    private static CommandNode<CommandSourceStack> convert(final CommandNode<FabricClientCommandSource> node, final Map<CommandNode<FabricClientCommandSource>, CommandNode<CommandSourceStack>> converted) {
        final CommandNode<CommandSourceStack> existing = converted.get(node);
        if (existing != null) {
            return existing;
        }
        final ArgumentBuilder<CommandSourceStack, ?> builder;
        if (node instanceof LiteralCommandNode<FabricClientCommandSource> literal) {
            builder = LiteralArgumentBuilder.<CommandSourceStack>literal(literal.getLiteral());
        } else if (node instanceof ArgumentCommandNode<FabricClientCommandSource, ?> argument) {
            builder = argument(argument);
        } else {
            throw new IllegalArgumentException("Unsupported command node " + node.getClass().getName());
        }
        builder.requires(source -> node.getRequirement().test(wrap(source)));
        if (node.getCommand() != null) {
            builder.executes(context -> node.getCommand().run(wrap(context)));
        }
        if (node.getRedirect() != null) {
            builder.redirect(convert(node.getRedirect(), converted));
        }
        final CommandNode<CommandSourceStack> built = builder.build();
        converted.put(node, built);
        for (final CommandNode<FabricClientCommandSource> child : node.getChildren()) {
            built.addChild(convert(child, converted));
        }
        return built;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentBuilder<CommandSourceStack, ?> argument(final ArgumentCommandNode<FabricClientCommandSource, ?> node) {
        final RequiredArgumentBuilder<CommandSourceStack, Object> builder = RequiredArgumentBuilder.argument(node.getName(), (ArgumentType<Object>) node.getType());
        final SuggestionProvider<FabricClientCommandSource> suggestions = node.getCustomSuggestions();
        if (suggestions != null) {
            builder.suggests((context, suggestionsBuilder) -> suggestions.getSuggestions(wrap(context), suggestionsBuilder));
        }
        return builder;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CommandContext<FabricClientCommandSource> wrap(final CommandContext<CommandSourceStack> context) {
        return ((CommandContext) context).copyFor(wrap(context.getSource()));
    }

    private static FabricClientCommandSource wrap(final CommandSourceStack source) {
        return new StackSource();
    }

    private static final class StackSource implements FabricClientCommandSource {
        private static Method SEND;

        @Override
        public void sendFeedback(final Component message) {
            final Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                send(client.player, message);
            }
        }

        private static void send(final LocalPlayer player, final Component message) {
            try {
                if (SEND == null) {
                    SEND = findSender();
                }
                if (SEND.getParameterCount() == 2) {
                    SEND.invoke(player, message, false);
                } else {
                    SEND.invoke(player, message);
                }
            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException("Could not send a client command message", e);
            }
        }

        private static Method findSender() {
            try {
                return LocalPlayer.class.getMethod("displayClientMessage", Component.class, boolean.class);
            } catch (final NoSuchMethodException ignored) {
                try {
                    return LocalPlayer.class.getMethod("sendSystemMessage", Component.class);
                } catch (final NoSuchMethodException e) {
                    throw new IllegalStateException("No way to send a client message on this Minecraft version", e);
                }
            }
        }

        @Override
        public void sendError(final Component error) {
            this.sendFeedback(Component.empty().append(error).withStyle(ChatFormatting.RED));
        }

        @Override
        public Minecraft getClient() {
            return Minecraft.getInstance();
        }

        @Override
        public LocalPlayer getPlayer() {
            return Minecraft.getInstance().player;
        }
    }
}
