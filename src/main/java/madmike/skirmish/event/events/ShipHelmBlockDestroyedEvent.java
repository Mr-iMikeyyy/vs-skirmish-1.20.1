package madmike.skirmish.event.events;

import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.MinecraftServer;

public class ShipHelmBlockDestroyedEvent {
    public static void register() {
        ShipHelmDestroyedCallback.EVENT.register((world, pos, state, player, ship) -> {
            if (ship == null) return;

            SkirmishManager manager = SkirmishManager.INSTANCE;

            MinecraftServer server = world.getServer();

            if (manager.isShipInSkirmish(ship.getId())) {
                manager.endSkirmishForShip(server, ship.getId());
            }
        });
    }
}
