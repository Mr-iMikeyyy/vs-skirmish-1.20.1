package madmike.skirmish.data;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record PlayerPreviousLocation (RegistryKey<World> dim, BlockPos pos) {
}
