package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import g_mungus.vlib.v2.api.VLibAPI;
import g_mungus.vlib.v2.api.extension.ShipExtKt;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.dimension.SkirmishDimension;
import madmike.skirmish.feature.blocks.SkirmishSpawnBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.ServerShip;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestExe {
    public static int executeTestSpawnBasic(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Must be a player to use this command"));
            return 0;
        }



        MinecraftServer server = ctx.getSource().getServer();

        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        if (skirmishDim == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not load skirmish dim");
            return 0;
        }

        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());
        if (party == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find party during test");
            return 0;
        }

        StructureTemplateManager stm = player.getServerWorld().getStructureTemplateManager();
        Identifier selfShipID = new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId());
        Optional<StructureTemplate> ship = stm.getTemplate(selfShipID);

        if (ship.isEmpty()) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find test ship");
            return 0;
        }

        BlockPos testPos = new BlockPos(0, 31, -100);
        VSSkirmish.LOGGER.info("[SKIRMISH] Placing Test Ship at {}", testPos);
        ServerShip testShip = VLibAPI.placeTemplateAsShip(ship.get(), skirmishDim, testPos, false);

        if (testShip == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Placed test ship but ship is null");
            return 0;
        }

        AtomicBoolean foundOppSpawn = new AtomicBoolean(false);
        BlockPos[] testSpawnPos = new BlockPos[1];

        ShipExtKt.forEachBlock(testShip, blockPos -> {
            if (foundOppSpawn.get()) return null;

            BlockState state = skirmishDim.getBlockState(blockPos);

            if (state.getBlock() instanceof SkirmishSpawnBlock) {
                foundOppSpawn.set(true);
                testSpawnPos[0] = blockPos;
                VSSkirmish.LOGGER.info("[SKIRMISH] FOUND Skirmish Spawn Block at {}", blockPos);
            }
            return null;
        });

        if (testSpawnPos[0] == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find spawn pos");
            return 0;
        }

        skirmishDim.getChunk(testSpawnPos[0].getX() >> 4, testSpawnPos[0].getZ() >> 4, ChunkStatus.FULL, true);

        player.teleport(skirmishDim, testSpawnPos[0].getX(), testSpawnPos[0].getY() + 2, testSpawnPos[0].getZ(), player.getYaw(), player.getPitch());

        player.setNoGravity(true);

        return 1;
    }

    public static int executeTestSpawnAdvanced(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Must be a player to use this command"));
            return 0;
        }

        MinecraftServer server = ctx.getSource().getServer();

        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        if (skirmishDim == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not load skirmish dim");
            return 0;
        }

        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());
        if (party == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find party during test");
            return 0;
        }

        StructureTemplateManager stm = player.getServerWorld().getStructureTemplateManager();
        Identifier selfShipID = new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId());
        Optional<StructureTemplate> ship = stm.getTemplate(selfShipID);

        if (ship.isEmpty()) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find test ship");
            return 0;
        }

        BlockPos testPos = new BlockPos(0, 31, -100);
        VSSkirmish.LOGGER.info("[SKIRMISH] Placing Test Ship at {}", testPos);
        ServerShip testShip = VLibAPI.placeTemplateAsShip(ship.get(), skirmishDim, testPos, false);

        if (testShip == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Placed test ship but ship is null");
            return 0;
        }

        AtomicBoolean foundOppSpawn = new AtomicBoolean(false);
        BlockPos[] testSpawnPos = new BlockPos[1];

        ShipExtKt.forEachBlock(testShip, blockPos -> {
            if (foundOppSpawn.get()) return null;

            BlockState state = skirmishDim.getBlockState(blockPos);

            if (state.getBlock() instanceof SkirmishSpawnBlock) {
                foundOppSpawn.set(true);
                testSpawnPos[0] = blockPos;
                VSSkirmish.LOGGER.info("[SKIRMISH] FOUND Skirmish Spawn Block at {}", blockPos);
            }
            return null;
        });

        if (testSpawnPos[0] == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find spawn pos");
            return 0;
        }

        skirmishDim.getChunk(testSpawnPos[0].getX() >> 4, testSpawnPos[0].getZ() >> 4, ChunkStatus.FULL, true);

        Vector3d testTpPos = testShip.getTransform().getShipToWorld().transformPosition(new Vector3d(testSpawnPos[0].getX(), testSpawnPos[0].getY(), testSpawnPos[0].getZ()));

        player.teleport(skirmishDim, testTpPos.x, testTpPos.y + 2, testTpPos.z, player.getYaw(), player.getPitch());

        player.setNoGravity(true);

        return 1;
    }

    public static int executeTestSpawnFloating(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Must be a player to use this command"));
            return 0;
        }

        MinecraftServer server = ctx.getSource().getServer();

        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        if (skirmishDim == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not load skirmish dim");
            return 0;
        }

        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());
        if (party == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find party during test");
            return 0;
        }

        StructureTemplateManager stm = player.getServerWorld().getStructureTemplateManager();
        Identifier selfShipID = new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId());
        Optional<StructureTemplate> ship = stm.getTemplate(selfShipID);

        if (ship.isEmpty()) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find test ship");
            return 0;
        }

        BlockPos testPos = new BlockPos(0, 31, -100);
        VSSkirmish.LOGGER.info("[SKIRMISH] Placing Test Ship at {}", testPos);
        ServerShip testShip = VLibAPI.placeTemplateAsShip(ship.get(), skirmishDim, testPos, false);

        if (testShip == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Placed test ship but ship is null");
            return 0;
        }

        BlockPos testSpawn = new BlockPos(0, 64, -130);

        skirmishDim.getChunk(testSpawn.getX() >> 4, testSpawn.getZ() >> 4, ChunkStatus.FULL, true);

        player.teleport(skirmishDim, testSpawn.getX(), testSpawn.getY(), testSpawn.getZ(), player.getYaw(), player.getPitch());

        player.setNoGravity(true);

        return 1;
    }

    public static int executeTestSpawnFloatingAdv(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Must be a player to use this command"));
            return 0;
        }

        MinecraftServer server = ctx.getSource().getServer();

        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        if (skirmishDim == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not load skirmish dim");
            return 0;
        }

        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());
        if (party == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find party during test");
            return 0;
        }

        StructureTemplateManager stm = player.getServerWorld().getStructureTemplateManager();
        Identifier selfShipID = new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId());
        Optional<StructureTemplate> ship = stm.getTemplate(selfShipID);

        if (ship.isEmpty()) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Could not find test ship");
            return 0;
        }

        BlockPos testPos = new BlockPos(0, 31, -100);
        VSSkirmish.LOGGER.info("[SKIRMISH] Placing Test Ship at {}", testPos);
        ServerShip testShip = VLibAPI.placeTemplateAsShip(ship.get(), skirmishDim, testPos, false);

        if (testShip == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Placed test ship but ship is null");
            return 0;
        }

        AABBdc worldAABB = testShip.getWorldAABB();

        double length = worldAABB.maxZ() - worldAABB.minZ();

        double safeDistance = length / 2.0 + 10.0; // 10 blocks of air buffer

        Vector3d shipCenter = worldAABB.center(new Vector3d());

        BlockPos testSpawn = new BlockPos(0, 64, (int) (shipCenter.z - safeDistance));

        skirmishDim.getChunk(testSpawn.getX() >> 4, testSpawn.getZ() >> 4, ChunkStatus.FULL, true);

        player.teleport(skirmishDim, testSpawn.getX(), testSpawn.getY(), testSpawn.getZ(), player.getYaw(), player.getPitch());

        player.setNoGravity(true);

        return 1;
    }

    public static int executeTestResetGravity(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Must be a player to use this command"));
            return 0;
        }

        player.setNoGravity(false);
        return 1;
    }

    public static int executeTestTrinkets(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Must be a player to use this command"));
            return 0;
        }

        TrinketComponent trinkets = TrinketsApi.getTrinketComponent(player).orElse(null);

        if (trinkets == null) {
            player.sendMessage(Text.literal("§cNo TrinketComponent found"));
            return 0;
        }

        trinkets.getInventory().forEach((groupName, slots) -> {
            slots.forEach((slotName, inventory) -> {
                for (int i = 0; i < inventory.size(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    if (!stack.isEmpty()) {
                        player.sendMessage(Text.literal(
                                "§7- " + groupName + "/" + slotName + ": §f" +
                                Registries.ITEM.getId(stack.getItem()) +
                                " x" + stack.getCount()
                        ));
                    }
                }
            });
        });
        return 1;
    }
}
