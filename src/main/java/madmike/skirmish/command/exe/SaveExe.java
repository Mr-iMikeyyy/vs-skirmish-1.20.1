package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import g_mungus.vlib.v2.api.VLibAPI;
import g_mungus.vlib.v2.api.extension.ShipExtKt;
import madmike.skirmish.VSSkirmish;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.eureka.block.BalloonBlock;
import org.valkyrienskies.eureka.block.ShipHelmBlock;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.concurrent.atomic.AtomicBoolean;

public class SaveExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        // Get party data
        OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
        IPartyManagerAPI pm = api.getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());

        if (party == null) {
            player.sendMessage(Text.literal("§cYou are not the owner of a party."), false);
            return 0;
        }

        // Get the ship the player is currently on

        ServerWorld world = player.getServerWorld();
        VSSkirmish.LOGGER.info("[SKIRMISH] Starting raycast routine for player {}", player.getName().getString());

// --- Camera start vec ---
        Vec3d start = player.getCameraPosVec(1.0f);
        VSSkirmish.LOGGER.info("[SKIRMISH] start vec = {}", start);

// --- End point (straight down 50 blocks) ---
        Vec3d end = start.add(0, -50, 0);
        VSSkirmish.LOGGER.info("[SKIRMISH] end vec = {}", end);

// --- Perform raycast ---
        VSSkirmish.LOGGER.info("[SKIRMISH] Performing raycast...");
        RaycastContext ray = new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        );

        BlockHitResult hit = world.raycast(ray);

        if (hit == null) {
            VSSkirmish.LOGGER.info("[SKIRMISH] world.raycast() returned NULL");
        } else {
            VSSkirmish.LOGGER.info("[SKIRMISH] hit type = {}", hit.getType());
            VSSkirmish.LOGGER.info("[SKIRMISH] hit pos = {}", hit.getPos());
        }

// --- Extract BlockPos ---
        BlockPos searchPos = null;

        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            searchPos = hit.getBlockPos();
            VSSkirmish.LOGGER.info("[SKIRMISH] BlockPos found = {}", searchPos);
        } else {
            VSSkirmish.LOGGER.info("[SKIRMISH] No BLOCK hit result found; searchPos is NULL");
        }

        if (searchPos == null) {
            VSSkirmish.LOGGER.info("[SKIRMISH] Could not compute a valid BlockPos; likely below world or in void");
            player.sendMessage(Text.literal("§cAre you in the void? Couldn't find a valid BlockPos."), false);
            return 0;
        }

// --- Try getting ship ---
        VSSkirmish.LOGGER.info("[SKIRMISH] Querying VS ship for BlockPos {}", searchPos);
        ServerShip ship = VSGameUtilsKt.getShipManagingPos(world, searchPos);

        if (ship == null) {
            VSSkirmish.LOGGER.info("[SKIRMISH] VS returned NULL ship for pos {}", searchPos);
            player.sendMessage(Text.literal("§cYou are not standing on a ship or there was an error saving it"), false);
            return 0;
        }

// --- Found ship ---
        VSSkirmish.LOGGER.info("[SKIRMISH] Ship FOUND! Ship ID = {}", ship.getId());
        VSSkirmish.LOGGER.info("[SKIRMISH] Ship successfully detected beneath player {}", player.getName().getString());

        // ============================================================
        // Scan for a Skirmish Spawn Block
        // ============================================================

        VSSkirmish.LOGGER.info("[SKIRMISH] Beginning scan for Skirmish Spawn Block on ship {}", ship.getId());

//        AtomicBoolean foundSpawn = new AtomicBoolean(false);
        AtomicBoolean foundHelm = new AtomicBoolean(false);
        AtomicBoolean foundBalloon = new AtomicBoolean(false);

//        BlockPos[] spawnPos = new BlockPos[1];

        ShipExtKt.forEachBlock(ship, blockPos -> {
            BlockState state = world.getBlockState(blockPos);
            Block block = state.getBlock();

            // Log scan
            VSSkirmish.LOGGER.info("[SKIRMISH] Scanning block at {} -> {}", blockPos, block);

            // Check no Balloon
            if (!foundBalloon.get() && block instanceof BalloonBlock) {
                VSSkirmish.LOGGER.info("[SKIRMISH] FOUND Balloon at {}", blockPos);
                foundBalloon.set(true);
            }

            // Check helm
            if (!foundHelm.get() && block instanceof ShipHelmBlock) {
                VSSkirmish.LOGGER.info("[SKIRMISH] FOUND ShipHelmBlock at {}", blockPos);
                foundHelm.set(true);
            }

            return null;
        });


        if (!foundHelm.get()) {
            VSSkirmish.LOGGER.info("[SKIRMISH] No Eureka Helm detected on ship {}", ship.getId());
            player.sendMessage(Text.literal("§cCould not detect a Helm"), false);
            return 0;
        }

        if (foundBalloon.get()) {
            VSSkirmish.LOGGER.info("[SKIRMISH] A Balloon was detected on ship {}", ship.getId());
            player.sendMessage(Text.literal("§cDetected a Balloon on the ship, no airships allowed"), false);
            return 0;
        }

// ============================================================
// Save ship template
// ============================================================

        Identifier filePath = new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId());
        VSSkirmish.LOGGER.info("[SKIRMISH] Saving ship {} to structure path {}", ship.getId(), filePath);

        try {
            VLibAPI.saveShipToTemplate(ship, filePath, world);
            VSSkirmish.LOGGER.info("[SKIRMISH] Ship {} saved successfully for party {}", ship.getId(), party.getId());
        } catch (Exception e) {
            VSSkirmish.LOGGER.error("[SKIRMISH] Error saving ship {} for party {}: {}", ship.getId(), party.getId(), e.getMessage());
            e.printStackTrace();
            player.sendMessage(Text.literal("§cThere was an error saving your ship. Check logs."), false);
            return 0;
        }

        player.sendMessage(Text.literal("§6[Skirmish Save] §7Saved your party's ship."), false);
        return 1;
    }
}
