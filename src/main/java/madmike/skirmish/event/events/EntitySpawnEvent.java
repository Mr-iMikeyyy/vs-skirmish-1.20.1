package madmike.skirmish.event.events;

import madmike.skirmish.config.SkirmishConfig;
import madmike.skirmish.dimension.SkirmishDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class EntitySpawnEvent {
    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world.getRegistryKey() == SkirmishDimension.SKIRMISH_LEVEL_KEY && !(entity instanceof ServerPlayerEntity)) {

                // Get the registry ID of the entity type
                Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());

                // If it's NOT whitelisted, remove it
                if (!SkirmishConfig.ENTITY_WHITELIST.contains(id.toString())) {
                    entity.discard();
                }
            }
        });
    }
}
