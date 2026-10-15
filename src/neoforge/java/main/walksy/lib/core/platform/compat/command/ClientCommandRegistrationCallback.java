package main.walksy.lib.core.platform.compat.command;

import com.mojang.brigadier.CommandDispatcher;
import main.walksy.lib.core.platform.compat.event.Event;
import main.walksy.lib.core.platform.compat.event.EventFactory;
import net.minecraft.commands.CommandBuildContext;

@FunctionalInterface
public interface ClientCommandRegistrationCallback {
    Event<ClientCommandRegistrationCallback> EVENT = EventFactory.createArrayBacked(ClientCommandRegistrationCallback.class, callbacks -> (dispatcher, registryAccess) -> {
        for (final ClientCommandRegistrationCallback callback : callbacks) {
            callback.register(dispatcher, registryAccess);
        }
    });

    void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess);
}
