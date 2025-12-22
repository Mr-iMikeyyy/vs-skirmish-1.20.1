package madmike.skirmish.logic;

import g_mungus.vlib.v2.api.VLibAPI;
import g_mungus.vlib.v2.api.extension.ShipExtKt;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.component.components.InventoryComponent;
import madmike.skirmish.component.components.RefundComponent;
import madmike.skirmish.component.components.ReturnPointComponent;
import madmike.skirmish.component.components.StatsComponent;
import madmike.skirmish.dimension.SkirmishDimension;
import madmike.skirmish.feature.blocks.SkirmishSpawnBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.chunk.ChunkStatus;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class SkirmishManager {

    private SkirmishChallenge currentChallenge;

    private Skirmish currentSkirmish;

    private int countdownTicks = 0;

    private boolean isCountingDown = false;

    private static final int CHECK_INTERVAL_TICKS = 200; // 10 seconds
    private static int tickCounter = 0;

    public static final SkirmishManager INSTANCE = new SkirmishManager();

    /* ================= Constructor ================= */

    private SkirmishManager() {}

    public SkirmishChallenge getCurrentChallenge() { return currentChallenge; }

    public Skirmish getCurrentSkirmish() {
        return currentSkirmish;
    }

    public void tick(MinecraftServer server) {
        if (currentSkirmish != null) {
            if (currentSkirmish.isExpired()) {
                endSkirmish(server, EndOfSkirmishType.TIME);
            }
            if (isCountingDown) {
                countdownTicks--;
                if (countdownTicks % 20 == 0) {
                    int secondsLeft = countdownTicks / 20;
                    if (secondsLeft != 0) {
                        currentSkirmish.broadcastMsg(server, "§eSkirmish starts in §c" + secondsLeft);
                    }
                    else {
                        isCountingDown = false;
                        currentSkirmish.broadcastMsg(server, "§eSkirmish has started! Fight!");
                        PlayerManager pm = server.getPlayerManager();
                        for (UUID id : currentSkirmish.getAllInvolvedPlayers()) {
                            ServerPlayerEntity player = pm.getPlayer(id);
                            if (player != null) {
                                player.setNoGravity(false);
                            }
                        }
                    }
                }
            }
//            tickCounter++;
//
//            if (tickCounter >= CHECK_INTERVAL_TICKS) {
//                tickCounter = 0;
//                currentSkirmish.runDistanceCheck(server);
//            }
        }
        if (currentChallenge != null) {
            if (currentChallenge.isExpired()) {
                currentChallenge.onExpire(server);
                currentChallenge = null;
            }
        }
    }

    public void startSkirmish(MinecraftServer server, SkirmishChallenge challenge) {

        VSSkirmish.LOGGER.info("[SKIRMISH] ===== Starting Skirmish =====");
        VSSkirmish.LOGGER.info("[SKIRMISH] Challenge: {}", challenge);

        // ============================================================
        // LOAD SKIRMISH DIMENSION
        // ============================================================
        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        VSSkirmish.LOGGER.info("[SKIRMISH] Loading Skirmish Dimension: {}", SkirmishDimension.SKIRMISH_LEVEL_KEY);

        if (skirmishDim == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Could not load skirmish dimension");
            challenge.broadcastMsg(server, "Error getting skirmish dimension, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        // ============================================================
        // PLACE CHALLENGER SHIP
        // ============================================================

        BlockPos chShipPos = new BlockPos(0, 31, -100);
        VSSkirmish.LOGGER.info("[SKIRMISH] Placing Challenger Ship at {}", chShipPos);
        ServerShip chShip = VLibAPI.placeTemplateAsShip(challenge.getChShipTemplate(), skirmishDim, chShipPos, false);

        if (chShip == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Failed to place Challenger ship!");
            challenge.broadcastMsg(server, "Error getting/placing challengers ship, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Challenger Ship placed successfully. Ship ID = {}", chShip.getId());

        // ============================================================
        // SCAN FOR CHALLENGER SPAWN BLOCK
        // ============================================================
//        VSSkirmish.LOGGER.info("[SKIRMISH] Scanning Challenger Ship for Skirmish Spawn Block...");
//
//        AtomicBoolean foundChSpawn = new AtomicBoolean(false);
//        BlockPos[] chSpawnPos = new BlockPos[1];
//
//        ShipExtKt.forEachBlock(chShip, blockPos -> {
//            if (foundChSpawn.get()) return null;
//
//            BlockState state = skirmishDim.getBlockState(blockPos);
//
//            VSSkirmish.LOGGER.info("[SKIRMISH] CH scan -> {} : {}", blockPos, state.getBlock());
//
//            if (state.getBlock() instanceof SkirmishSpawnBlock) {
//                foundChSpawn.set(true);
//                chSpawnPos[0] = blockPos;
//                VSSkirmish.LOGGER.info("[SKIRMISH] FOUND CH Skirmish Spawn Block at {}", blockPos);
//            }
//            return null;
//        });
//
//        if (chSpawnPos[0] == null) {
//            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Challenger Ship missing Skirmish Spawn Block!");
//            challenge.broadcastMsg(server,"Could not detect a Skirmish Spawn Block on challenger ship, this should never happen, cancelling skirmish");
//            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
//            currentChallenge = null;
//            return;
//        }

        // ============================================================
        // PLACE OPPONENT SHIP
        // ============================================================
        BlockPos oppShipPos = new BlockPos(0, 31, 100);
        VSSkirmish.LOGGER.info("[SKIRMISH] Placing Opponent Ship at {}", oppShipPos);
        ServerShip oppShip = VLibAPI.placeTemplateAsShip(challenge.getOppShipTemplate(), skirmishDim, oppShipPos, false);

        if (oppShip == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Failed to place Opponent ship!");
            challenge.broadcastMsg(server, "Error getting/placing opponents ship, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Opponent Ship placed successfully. Ship ID = {}", oppShip.getId());

        // ============================================================
        // SCAN FOR OPPONENT SPAWN BLOCK
        // ============================================================
//        VSSkirmish.LOGGER.info("[SKIRMISH] Scanning Opponent Ship for Skirmish Spawn Block...");
//
//        AtomicBoolean foundOppSpawn = new AtomicBoolean(false);
//        BlockPos[] oppSpawnPos = new BlockPos[1];
//
//        ShipExtKt.forEachBlock(oppShip, blockPos -> {
//            if (foundOppSpawn.get()) return null;
//
//            BlockState state = skirmishDim.getBlockState(blockPos);
//
//            VSSkirmish.LOGGER.info("[SKIRMISH] OPP scan -> {} : {}", blockPos, state.getBlock());
//
//            if (state.getBlock() instanceof SkirmishSpawnBlock) {
//                foundOppSpawn.set(true);
//                oppSpawnPos[0] = blockPos;
//                VSSkirmish.LOGGER.info("[SKIRMISH] FOUND OPP Skirmish Spawn Block at {}", blockPos);
//            }
//            return null;
//        });
//
//        if (oppSpawnPos[0] == null) {
//            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Opponent Ship missing Skirmish Spawn Block!");
//            challenge.broadcastMsg(server,"Could not detect a Skirmish Spawn Block on opponent ship, this should never happen, cancelling skirmish");
//            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
//            currentChallenge = null;
//            return;
//        }

        // ============================================================
        // LOAD PARTIES & TELEPORT PLAYERS
        // ============================================================

        VSSkirmish.LOGGER.info("[SKIRMISH] Fetching party info...");

        Scoreboard sb = server.getScoreboard();
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();

        IServerPartyAPI chParty = pm.getPartyById(challenge.getChPartyId());
        if (chParty == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Challenger party not found!");
            challenge.broadcastMsg(server,"Error getting challenging party, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(sb).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        IServerPartyAPI oppParty = pm.getPartyById(challenge.getOppPartyId());
        if (oppParty == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Opponent party not found!");
            challenge.broadcastMsg(server,"Error getting opponent party, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(sb).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        AABBdc chShipWorldAABB = chShip.getWorldAABB();

        double chLength = chShipWorldAABB.maxZ() - chShipWorldAABB.minZ();

        double chSafe = chLength / 2.0 + 10.0; // 10 blocks of air buffer

        Vector3d chShipCenter = chShipWorldAABB.center(new Vector3d());

        BlockPos chSpawn = new BlockPos(0, 64, (int) (chShipCenter.z - chSafe));

        skirmishDim.getChunk(chSpawn.getX() >> 4, chSpawn.getZ() >> 4, ChunkStatus.FULL, true);

        // Challenger teleports
        Set<UUID> challengerIds = new HashSet<>();
        VSSkirmish.LOGGER.info("[SKIRMISH] Teleporting Challenger Party: {}", chParty.getId());

        chParty.getOnlineMemberStream().forEach(player -> {
            player.setNoGravity(true);
            challengerIds.add(player.getUuid());

            VSSkirmish.LOGGER.info("[SKIRMISH] Saving return point for CH player {}", player.getGameProfile().getName());
            SkirmishComponents.RETURN_POINTS.get(sb).set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());

            VSSkirmish.LOGGER.info("[SKIRMISH] Saving inventory for CH player {}", player.getGameProfile().getName());
            SkirmishComponents.INVENTORY.get(sb).saveInventory(player);

            VSSkirmish.LOGGER.info("[SKIRMISH] Teleporting CH {} to recalculated {}", player.getGameProfile().getName(), chSpawn);
            player.teleport(skirmishDim, chSpawn.getX(), chSpawn.getY(), chSpawn.getZ(), player.getYaw(), player.getPitch());
        });

        // Opponent teleports


        AABBdc oppShipWorldAABB = oppShip.getWorldAABB();

        double oppLength = oppShipWorldAABB.maxZ() - oppShipWorldAABB.minZ();

        double oppSafe = oppLength / 2.0 + 10.0; // 10 blocks of air buffer

        Vector3d oppShipCenter = oppShipWorldAABB.center(new Vector3d());

        BlockPos oppSpawn = new BlockPos(0, 64, (int) (oppShipCenter.z + oppSafe));

        skirmishDim.getChunk(oppSpawn.getX() >> 4, oppSpawn.getZ() >> 4, ChunkStatus.FULL, true);

        Set<UUID> opponentIds = new HashSet<>();
        VSSkirmish.LOGGER.info("[SKIRMISH] Teleporting Opponent Party: {}", oppParty.getId());

        oppParty.getOnlineMemberStream().forEach(player -> {
            player.setNoGravity(true);
            opponentIds.add(player.getUuid());

            VSSkirmish.LOGGER.info("[SKIRMISH] Saving return point for OPP player {}", player.getGameProfile().getName());
            SkirmishComponents.RETURN_POINTS.get(sb).set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());

            VSSkirmish.LOGGER.info("[SKIRMISH] Saving inventory for OPP player {}", player.getGameProfile().getName());
            SkirmishComponents.INVENTORY.get(sb).saveInventory(player);

            VSSkirmish.LOGGER.info("[SKIRMISH] Teleporting OPP {} to {}", player.getGameProfile().getName(), oppSpawn);
            player.teleport(skirmishDim, oppSpawn.getX(), oppSpawn.getY(), oppSpawn.getZ(), player.getYaw(), player.getPitch());
        });

        // ============================================================
        // CREATE SKIRMISH
        // ============================================================
        VSSkirmish.LOGGER.info("[SKIRMISH] Constructing Skirmish object...");

        currentSkirmish = new Skirmish(
                challengerIds,
                chParty.getId(),
                challenge.getChLeaderId(),
                opponentIds,
                oppParty.getId(),
                challenge.getOppLeaderId(),
                chShip.getId(),
                oppShip.getId(),
                challenge.getWager()
        );

        VSSkirmish.LOGGER.info("[SKIRMISH] Skirmish created successfully!");

        // ============================================================
        // BROADCAST
        // ============================================================
        challenge.broadcastMsg(server, "Skirmish Started!");

        server.getPlayerManager().getPlayerList().forEach(player -> {
            UUID playerId = player.getUuid();
            if (!challengerIds.contains(playerId) && !opponentIds.contains(playerId)) {
                player.sendMessage(Text.literal("A skirmish has started! Use /skirmish spectate to watch!"));
            }
        });

        startCountdown(5);

        VSSkirmish.LOGGER.info("[SKIRMISH] ===== Skirmish Started Successfully =====");
        currentChallenge = null;
    }

    public void startCountdown(int seconds) {
        this.countdownTicks = seconds * 20;
        isCountingDown = true;
    }

    public void endSkirmish(MinecraftServer server, EndOfSkirmishType type) {
        VSSkirmish.LOGGER.info("[SKIRMISH] ===== Ending Skirmish =====");

        if (currentSkirmish == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Tried to end skirmish but currentSkirmish is NULL!");
            return;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Skirmish Type = {}", type);
        VSSkirmish.LOGGER.info("[SKIRMISH] Challenger Party ID = {}", currentSkirmish.getChPartyId());
        VSSkirmish.LOGGER.info("[SKIRMISH] Opponent Party ID = {}", currentSkirmish.getOppPartyId());

        // ============================================================
        // GET DIMENSION
        // ============================================================
        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        if (skirmishDim == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Skirmish dimension is NULL");
            return;
        }

        // ============================================================
        // STATS + REFUNDS
        // ============================================================
        StatsComponent sc = SkirmishComponents.STATS.get(server.getScoreboard());
        RefundComponent rc = SkirmishComponents.REFUNDS.get(server.getScoreboard());

        VSSkirmish.LOGGER.info("[SKIRMISH] Applying results + stats for type {}", type);

        switch (type) {

            case CHALLENGERS_WIN_KILLS -> {
                VSSkirmish.LOGGER.info("[SKIRMISH] Result: Challengers win by kills");

                sc.setPartySkirmishStats(currentSkirmish.getChPartyId(), 1, 0,
                        currentSkirmish.getWager(), 0, 0, 0);

                sc.setPartySkirmishStats(currentSkirmish.getOppPartyId(), 0, 1,
                        0, currentSkirmish.getWager(), 0, 0);

                rc.refundPlayer(server, currentSkirmish.getChLeaderId(),
                        currentSkirmish.getWager() * 20000L);

                currentSkirmish.broadcastMsg(server, "Challengers won by kills!");
            }

            case OPPONENTS_WIN_KILLS -> {
                VSSkirmish.LOGGER.info("[SKIRMISH] Result: Opponents win by kills");

                sc.setPartySkirmishStats(currentSkirmish.getChPartyId(), 0, 1,
                        0, currentSkirmish.getWager(), 0, 0);

                sc.setPartySkirmishStats(currentSkirmish.getOppPartyId(), 1, 0,
                        currentSkirmish.getWager(), 0, 0, 0);

                rc.refundPlayer(server, currentSkirmish.getOppLeaderId(),
                        currentSkirmish.getWager() * 20000L);

                currentSkirmish.broadcastMsg(server, "Opponents won by kills!");
            }

            case CHALLENGERS_WIN_SHIP -> {
                VSSkirmish.LOGGER.info("[SKIRMISH] Result: Challengers win by ship destruction");

                sc.setPartySkirmishStats(currentSkirmish.getChPartyId(), 1, 0,
                        currentSkirmish.getWager(), 0, 0, 1);

                sc.setPartySkirmishStats(currentSkirmish.getOppPartyId(), 0, 1,
                        0, currentSkirmish.getWager(), 1, 0);

                rc.refundPlayer(server, currentSkirmish.getChLeaderId(),
                        currentSkirmish.getWager() * 20000L);

                currentSkirmish.broadcastMsg(server, "Challengers won by sinking the ship!");
            }

            case OPPONENTS_WIN_SHIP -> {
                VSSkirmish.LOGGER.info("[SKIRMISH] Result: Opponents win by ship destruction");

                sc.setPartySkirmishStats(currentSkirmish.getChPartyId(), 0, 1,
                        0, currentSkirmish.getWager(), 1, 0);

                sc.setPartySkirmishStats(currentSkirmish.getOppPartyId(), 1, 0,
                        currentSkirmish.getWager(), 0, 0, 1);

                rc.refundPlayer(server, currentSkirmish.getOppLeaderId(),
                        currentSkirmish.getWager() * 20000L);

                currentSkirmish.broadcastMsg(server, "Opponents won by sinking the ship!");
            }

            case TIME -> {
                VSSkirmish.LOGGER.info("[SKIRMISH] Result: Time ran out");

                rc.refundPlayer(server, currentSkirmish.getChLeaderId(),
                        currentSkirmish.getWager());

                rc.refundPlayer(server, currentSkirmish.getOppLeaderId(),
                        currentSkirmish.getWager());

                currentSkirmish.broadcastMsg(server, "The time ran out on the skirmish!");
            }
        }

        // ============================================================
        // TELEPORT PLAYERS BACK & RESTORE INVENTORY
        // ============================================================
        Set<UUID> players = currentSkirmish.getAllInvolvedPlayers();
        PlayerManager pm = server.getPlayerManager();

        InventoryComponent ic = SkirmishComponents.INVENTORY.get(server.getScoreboard());
        ReturnPointComponent rpc = SkirmishComponents.RETURN_POINTS.get(server.getScoreboard());

        VSSkirmish.LOGGER.info("[SKIRMISH] Teleporting {} players back...", players.size());

        for (UUID id : players) {
            ServerPlayerEntity player = pm.getPlayer(id);
            if (player != null) {

                VSSkirmish.LOGGER.info("[SKIRMISH] Restoring {} (UUID={})",
                        player.getGameProfile().getName(), id);

                ic.restoreInventory(player);
                rpc.tpPlayerBack(player);
            } else {
                VSSkirmish.LOGGER.warn("[SKIRMISH] Player UUID {} was offline during restore", id);
            }
        }

        // ============================================================
        // DELETE SKIRMISH SHIPS
        // ============================================================
        VSSkirmish.LOGGER.info("[SKIRMISH] Removing skirmish ships...");

        List<Ship> ships = VSGameUtilsKt.getAllShips(skirmishDim).stream().toList();
        for (Ship ship : ships) {
            if (ship instanceof ServerShip serverShip) {
                long id = serverShip.getId();
                if (id == currentSkirmish.getChShipId() || id == currentSkirmish.getOppShipId()) {
                    VSSkirmish.LOGGER.info("[SKIRMISH] Deleting ship {}", id);
                    VLibAPI.discardShip(serverShip, skirmishDim);
                }
            }
        }

        // ============================================================
        // WIPE DIMENSION REGION CONTENTS
        // ============================================================
        VSSkirmish.LOGGER.info("[SKIRMISH] Wiping skirmish dimension region files...");

        Path dimPath = server.getSavePath(WorldSavePath.ROOT)
                .resolve("dimensions")
                .resolve("vs-skirmish")
                .resolve("skirmish_dim")
                .resolve("region");

        if (!Files.exists(dimPath)) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Skirmish region folder does not exist: {}", dimPath);
            currentSkirmish = null;
            return;
        }

        try (Stream<Path> stream = Files.walk(dimPath)) {
            stream.filter(p -> p.toString().endsWith(".mca"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            VSSkirmish.LOGGER.info("[SKIRMISH] Deleted region: {}", path);
                        } catch (IOException e) {
                            VSSkirmish.LOGGER.error("[SKIRMISH] Failed to delete {}", path, e);
                        }
                    });
        } catch (IOException e) {
            VSSkirmish.LOGGER.error("[SKIRMISH] IOException during dimension wipe", e);
        }

        tickCounter = 0;
        countdownTicks = 0;
        currentSkirmish = null;
        VSSkirmish.LOGGER.info("[SKIRMISH] ===== Skirmish End Complete =====");
    }

    public boolean hasChallengeOrSkirmish() {
        return currentSkirmish != null || currentChallenge != null;
    }

    public boolean handlePlayerDeath(ServerPlayerEntity player) {
        if (currentSkirmish != null) {
            return currentSkirmish.handlePlayerDeath(player);
        }
        return true;
    }

    public void setCurrentChallenge(SkirmishChallenge challenge) {
        this.currentChallenge = challenge;
    }

    public boolean isShipInSkirmish(long id) {
        if (currentSkirmish == null) {
            return false;
        }
        return currentSkirmish.isShipInSkirmish(id);
    }

    public void endSkirmishForShip(MinecraftServer server, long id) {
        if (currentSkirmish.isChallengerShip(id)) {
            endSkirmish(server, EndOfSkirmishType.OPPONENTS_WIN_SHIP);
        }
        else {
            endSkirmish(server, EndOfSkirmishType.CHALLENGERS_WIN_SHIP);
        }
    }

    public void handlePlayerQuit(MinecraftServer server, ServerPlayerEntity player) {
        if (currentChallenge != null) {
            currentChallenge.handlePlayerQuit(server, player);
        }
        if (currentSkirmish != null) {
            currentSkirmish.handlePlayerQuit(player);
        }
    }

    public boolean isMovementLocked(UUID playerId) {
        if (currentSkirmish == null) {
            return false;
        }
        if (countdownTicks <= 0) {
            return false;
        }
        return currentSkirmish.isPlayerInSkirmish(playerId);
    }
}
