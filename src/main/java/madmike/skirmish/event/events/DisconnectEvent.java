package madmike.skirmish.event.events;

import madmike.skirmish.logic.SkirmishChallengeManager;
import madmike.skirmish.logic.SkirmishManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class DisconnectEvent {
    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register(((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            SkirmishManager.INSTANCE.handlePlayerQuit(server, player);
            SkirmishChallengeManager.INSTANCE.handlePlayerQuit(server, player);
        }));
    }
}
