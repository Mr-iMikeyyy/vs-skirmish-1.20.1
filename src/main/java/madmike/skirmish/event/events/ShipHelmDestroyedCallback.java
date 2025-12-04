package madmike.skirmish.event.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.valkyrienskies.core.api.ships.Ship;

public interface ShipHelmDestroyedCallback {
    Event<ShipHelmDestroyedCallback> EVENT = EventFactory.createArrayBacked(
            ShipHelmDestroyedCallback.class,
            listeners -> (world, pos, state, player, ship) -> {
                for (var listener : listeners) {
                    listener.onHelmDestroyed(world, pos, state, player, ship);
                }
            }
    );

    void onHelmDestroyed(World world,
                         BlockPos pos,
                         BlockState state,
                         ServerPlayerEntity player,
                         Ship ship);
}
