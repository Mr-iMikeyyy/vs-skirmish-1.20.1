package madmike.skirmish.feature;

import madmike.skirmish.VSSkirmish;
import madmike.skirmish.feature.blocks.SkirmishSpawnBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SkirmishBlocks {
    public static final Block SKIRMISH_SPAWN_BLOCK = registerBlock(
            "skirmish_spawn_block",
            new SkirmishSpawnBlock(
                    FabricBlockSettings.create()
                            .strength(3.0F, 6.0F) // hardness, blast resistance
                            .requiresTool()        // needs pickaxe or whatever you configure
            )
    );

    private SkirmishBlocks() {
        // no-op
    }

    private static Block registerBlock(String name, Block block) {
        try {
            Identifier id = new Identifier(VSSkirmish.MOD_ID, name);

            BlockItem blockItem = new BlockItem(block, new FabricItemSettings());
            Registry.register(Registries.ITEM, id, blockItem);

            VSSkirmish.LOG.info("Registered block: {}", name);
            return Registry.register(Registries.BLOCK, id, block);
        } catch (Exception e) {
            VSSkirmish.LOG.error("Failed to register block: {}", name, e);
            throw e;
        }
    }

    public static void init() {
        try {
            VSSkirmish.LOG.info("Initializing Skirmish blocks");
        } catch (Exception e) {
            VSSkirmish.LOG.error("Failed to initialize Skirmish blocks", e);
        }
    }
}
