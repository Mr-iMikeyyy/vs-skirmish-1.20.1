package madmike.skirmish.mixin;

import madmike.skirmish.VSSkirmish;
import madmike.skirmish.event.events.ShipHelmDestroyedCallback;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.eureka.block.ShipHelmBlock;

@Mixin(ShipHelmBlock.class)
public class ShipHelmBlockMixin {

    @Inject(
            method = "onStateReplaced",
            at = @At("HEAD")
    )
    private void onHelmRemoved(
            BlockState state, World level, BlockPos pos, BlockState newState, boolean isMoving, CallbackInfo ci
    ) {
        try {
            if (state.getBlock() == newState.getBlock()) return;

            Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
            VSSkirmish.LOG.info("Ship helm destroyed at position: {}", pos);

            ShipHelmDestroyedCallback.EVENT.invoker()
                    .onHelmDestroyed(level, pos, state, null, ship);
        } catch (Exception e) {
            VSSkirmish.LOG.error("Error handling helm destruction at position: {}", pos, e);
        }
    }
}
