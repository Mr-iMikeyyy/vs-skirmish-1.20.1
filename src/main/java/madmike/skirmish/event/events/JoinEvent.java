package madmike.skirmish.event.events;

import madmike.skirmish.component.SkirmishComponents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

public class JoinEvent {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(((serverPlayNetworkHandler, packetSender, server) -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();

            boolean isOp = server.getPlayerManager().isOperator(player.getGameProfile());

            if (!isOp) {
                player.changeGameMode(GameMode.SURVIVAL);
            }

            SkirmishComponents.NAMES.get(server.getScoreboard()).onPlayerLogin(player);

            SkirmishComponents.RETURN_POINTS.get(server.getScoreboard()).tpPlayerBack(player);

            SkirmishComponents.REFUNDS.get(server.getScoreboard()).onPlayerLogin(player);

            SkirmishComponents.INVENTORY.get(server.getScoreboard()).restoreInventory(player);

        }));
    }
}
