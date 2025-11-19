package madmike.skirmish.event.events;

import madmike.skirmish.dimension.SkirmishDimension;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class BlockDropEvent {
    public static void register() {
        // Step 1: Cancel all breaks in duel dimension
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.getRegistryKey() == SkirmishDimension.SKIRMISH_LEVEL_KEY) {
                // Returning false cancels the event, which triggers the CANCELED callback next
                return false;
            }
            return true;
        });

        // Step 2: Handle canceled breaks manually
        PlayerBlockBreakEvents.CANCELED.register((world, player, pos, state, blockEntity) -> {
            if (world.getRegistryKey() == SkirmishDimension.SKIRMISH_LEVEL_KEY) {
                // Ensure this only runs on server
                if (!world.isClient) {
                    // Remove block without drops
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);

                    // Force chunk + block update so all players see it
                    world.updateListeners(pos, state, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

                    // Optional: play break particles/sound
                    world.syncWorldEvent(null, 2001, pos, Block.getRawIdFromState(state));
                }
            }
        });
    }
}
