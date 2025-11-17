package madmike.skirmish.event.events;

import madmike.skirmish.logic.Skirmish;
import madmike.skirmish.logic.SkirmishManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class AllowDeathEvent {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((livingEntity, damageSource, v) -> {
            if (!(livingEntity instanceof ServerPlayerEntity player)) {
                return true; // Only handle players
            }

            return SkirmishManager.INSTANCE.handlePlayerDeath(player);
        });
    }
}
