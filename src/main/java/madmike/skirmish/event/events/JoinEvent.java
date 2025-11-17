package madmike.skirmish.event.events;

import madmike.skirmish.component.SkirmishComponents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class JoinEvent {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(((serverPlayNetworkHandler, packetSender, server) -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();

            SkirmishComponents.NAMES.get(server.getScoreboard()).onPlayerLogin(player);

            SkirmishComponents.RETURN_POINTS.get(server.getScoreboard()).onPlayerLogin(player);

            SkirmishComponents.REFUNDS.get(server.getScoreboard()).onPlayerLogin(player);

        }));
    }
}
