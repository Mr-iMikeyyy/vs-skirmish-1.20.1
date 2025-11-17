package madmike.skirmish.dimension;

import madmike.skirmish.VSSkirmish;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;

public class SkirmishDimension {
    public static final RegistryKey<DimensionOptions> SKIRMISH_DIM_KEY = RegistryKey.of(RegistryKeys.DIMENSION,
            new Identifier(VSSkirmish.MOD_ID, "skirmish_dim"));
    public static final RegistryKey<World> SKIRMISH_LEVEL_KEY = RegistryKey.of(RegistryKeys.WORLD,
            new Identifier(VSSkirmish.MOD_ID, "skirmish_dim"));
    public static final RegistryKey<DimensionType> SKIRMISH_DIM_TYPE = RegistryKey.of(RegistryKeys.DIMENSION_TYPE,
            new Identifier(VSSkirmish.MOD_ID, "skirmish_dim_type"));
}
