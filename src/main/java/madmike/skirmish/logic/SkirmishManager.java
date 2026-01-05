package madmike.skirmish.logic;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import g_mungus.vlib.v2.api.VLibAPI;
import madmike.cc.component.CCComponents;
import madmike.cc.component.components.BankComponent;
import madmike.cc.component.components.InventoryComponent;
import madmike.cc.component.components.TeleportComponent;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.component.components.StatsComponent;
import madmike.skirmish.dimension.SkirmishDimension;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
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
import java.util.stream.Stream;

public class SkirmishManager {

    private Skirmish currentSkirmish;

    private static final int CHECK_INTERVAL_TICKS = 200; // 10 seconds
    private static int tickCounter = 0;

    public static final SkirmishManager INSTANCE = new SkirmishManager();

    /* ================= Constructor ================= */

    private SkirmishManager() {}

    public Skirmish getCurrentSkirmish() {
        return currentSkirmish;
    }

    public void tick(MinecraftServer server) {
        if (currentSkirmish != null) {
            if (currentSkirmish.isExpired()) {
                endSkirmish(server, EndOfSkirmishType.TIME);
            }
            if (currentSkirmish.getIsCountingDown()) {
                currentSkirmish.tickCountdown();
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
    }

    public void tryToStartSkirmish(MinecraftServer server, ServerPlayerEntity chPartyLeader, ServerPlayerEntity oppPartyLeader) {
        SkirmishChallenge challenge = SkirmishChallengeManager.INSTANCE.getOldestAcceptedChallenge();

        if (challenge == null) {
            return;
        }

        Skirmish skirmish = new Skirmish();

        boolean started = skirmish.start(server, challenge);

        if (started) {
            currentSkirmish = skirmish;
        }

        // ============================================================
        // CHECK CHALLENGE
        // ============================================================

        if (currentChallenge == null) {
            return;
        }

        // ============================================================
        // CHECK MONEY
        // ============================================================

        CurrencyComponent chWallet = ModComponents.CURRENCY.get(chPartyLeader);
        CurrencyComponent oppWallet = ModComponents.CURRENCY.get(oppPartyLeader);
        long bet = currentChallenge.getWager() * 10000L;
        if (chWallet.getValue() < bet) {
            currentChallenge.end(server, "Challenger didn't have enough gold in their wallet");
            return;
        }
        if (oppWallet.getValue() < bet) {
            currentChallenge.end(server, "Opponent didn't have enough gold in their wallet");
            return;
        }

        // ============================================================
        // LOAD SKIRMISH DIMENSION
        // ============================================================
        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);

        if (skirmishDim == null) {
            currentChallenge.end(server, "Couldn't load skirmish dimension");
            return;
        }

        // ============================================================
        // PLACE CHALLENGER SHIP
        // ============================================================

        BlockPos chShipPos = new BlockPos(0, 31, -100);
        ServerShip chShip = VLibAPI.placeTemplateAsShip(currentChallenge.getChShipTemplate(), skirmishDim, chShipPos, false);

        if (chShip == null) {
            currentChallenge.end(server, "Couldn't load challengers ship");
            return;
        }

        // ============================================================
        // PLACE OPPONENT SHIP
        // ============================================================
        BlockPos oppShipPos = new BlockPos(0, 31, 100);
        ServerShip oppShip = VLibAPI.placeTemplateAsShip(currentChallenge.getOppShipTemplate(), skirmishDim, oppShipPos, false);

        if (oppShip == null) {
            currentChallenge.end(server, "Couldn't load opponents ship");
            return;
        }

        // ============================================================
        // LOAD PARTIES & TELEPORT PLAYERS
        // ============================================================

        Scoreboard sb = server.getScoreboard();
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();

        IServerPartyAPI chParty = pm.getPartyById(currentChallenge.getChPartyId());
        if (chParty == null) {
            currentChallenge.end(server, "Couldn't load challenging party");
            return;
        }

        IServerPartyAPI oppParty = pm.getPartyById(currentChallenge.getOppPartyId());
        if (oppParty == null) {
            currentChallenge.end(server, "Couldn't load opponent party");
            return;
        }

        // Challenger teleports

        TeleportComponent tp = CCComponents.TP.get(sb);
        InventoryComponent inv = CCComponents.INV.get(sb);

        AABBdc chShipWorldAABB = chShip.getWorldAABB();
        double chLength = chShipWorldAABB.maxZ() - chShipWorldAABB.minZ();
        double chSafe = chLength / 2.0 + 10.0; // 10 blocks of air buffer
        Vector3d chShipCenter = chShipWorldAABB.center(new Vector3d());
        BlockPos chSpawn = new BlockPos(0, 64, (int) (chShipCenter.z - chSafe));
        skirmishDim.getChunk(chSpawn.getX() >> 4, chSpawn.getZ() >> 4, ChunkStatus.FULL, true);
        Set<UUID> challengerIds = new HashSet<>();

        chParty.getOnlineMemberStream().forEach(player -> {
            player.setNoGravity(true);
            challengerIds.add(player.getUuid());

            tp.set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());

            inv.saveInventory(player);

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

        oppParty.getOnlineMemberStream().forEach(player -> {
            player.setNoGravity(true);
            opponentIds.add(player.getUuid());

            tp.set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());

            inv.saveInventory(player);

            player.teleport(skirmishDim, oppSpawn.getX(), oppSpawn.getY(), oppSpawn.getZ(), player.getYaw(), player.getPitch());
        });

        // ============================================================
        // CREATE SKIRMISH AND TAKE MONEY
        // ============================================================

        currentSkirmish = new Skirmish(
                challengerIds,
                chParty.getId(),
                currentChallenge.getChLeaderId(),
                opponentIds,
                oppParty.getId(),
                currentChallenge.getOppLeaderId(),
                chShip.getId(),
                oppShip.getId(),
                currentChallenge.getWager()
        );

        ModComponents.CURRENCY.get(chPartyLeader).modify(-bet);
        ModComponents.CURRENCY.get(oppPartyLeader).modify(-bet);

        CCComponents.BANK.get(sb).record(currentSkirmish.getId(), currentChallenge.getChLeaderId(), currentChallenge.getOppLeaderId(), bet);

        currentChallenge = null;
        // ============================================================
        // BROADCAST
        // ============================================================
        currentSkirmish.broadcastMsg(server, "Get Ready!");

        server.getPlayerManager().getPlayerList().forEach(player -> {
            UUID playerId = player.getUuid();
            if (!challengerIds.contains(playerId) && !opponentIds.contains(playerId)) {
                player.sendMessage(Text.literal("A skirmish has started! Use /skirmish spectate to watch!"));
            }
        });

        startCountdown(5);
    }



    public void endSkirmish(MinecraftServer server, EndOfSkirmishType type) {

        if (currentSkirmish == null) {
            return;
        }

        // ============================================================
        // STATS + REFUNDS
        // ============================================================
        StatsComponent sc = SkirmishComponents.STATS.get(server.getScoreboard());
        BankComponent bank = CCComponents.BANK.get(server.getScoreboard());

        switch (type) {

            case CHALLENGERS_WIN_KILLS -> {

                sc.setPartySkirmishStats(currentSkirmish.getChPartyId(), 1, 0,
                        currentSkirmish.getWager(), 0, 0, 0);

                sc.setPartySkirmishStats(currentSkirmish.getOppPartyId(), 0, 1,
                        0, currentSkirmish.getWager(), 0, 0);

                bank.reward(currentSkirmish.getId(), currentSkirmish.getChLeaderId());

                currentSkirmish.broadcastMsg(server, "Challengers won by kills!");
            }

            case OPPONENTS_WIN_KILLS -> {

                sc.setPartySkirmishStats(currentSkirmish.getChPartyId(), 0, 1,
                        0, currentSkirmish.getWager(), 0, 0);

                sc.setPartySkirmishStats(currentSkirmish.getOppPartyId(), 1, 0,
                        currentSkirmish.getWager(), 0, 0, 0);

                bank.reward(currentSkirmish.getId(), currentSkirmish.getOppLeaderId());

                currentSkirmish.broadcastMsg(server, "Opponents won by kills!");
            }

            case CHALLENGERS_WIN_SHIP -> {

                sc.setPartySkirmishStats(currentSkirmish.getChPartyId(), 1, 0,
                        currentSkirmish.getWager(), 0, 0, 1);

                sc.setPartySkirmishStats(currentSkirmish.getOppPartyId(), 0, 1,
                        0, currentSkirmish.getWager(), 1, 0);

                bank.reward(currentSkirmish.getId(), currentSkirmish.getChLeaderId());

                currentSkirmish.broadcastMsg(server, "Challengers won by sinking the ship!");
            }

            case OPPONENTS_WIN_SHIP -> {
                VSSkirmish.LOGGER.info("[SKIRMISH] Result: Opponents win by ship destruction");

                sc.setPartySkirmishStats(currentSkirmish.getChPartyId(), 0, 1,
                        0, currentSkirmish.getWager(), 1, 0);

                sc.setPartySkirmishStats(currentSkirmish.getOppPartyId(), 1, 0,
                        currentSkirmish.getWager(), 0, 0, 1);

                bank.reward(currentSkirmish.getId(), currentSkirmish.getOppLeaderId());

                currentSkirmish.broadcastMsg(server, "Opponents won by sinking the ship!");
            }

            case TIME -> {
                VSSkirmish.LOGGER.info("[SKIRMISH] Result: Time ran out");

                bank.reward(currentSkirmish.getId(), null);

                currentSkirmish.broadcastMsg(server, "The time ran out on the skirmish!");
            }
        }

        // ============================================================
        // TELEPORT PLAYERS BACK & RESTORE INVENTORY
        // ============================================================
        Set<UUID> players = currentSkirmish.getAllInvolvedPlayers();
        PlayerManager pm = server.getPlayerManager();

        InventoryComponent ic = CCComponents.INV.get(server.getScoreboard());
        TeleportComponent tc = CCComponents.TP.get(server.getScoreboard());

        for (UUID id : players) {
            ServerPlayerEntity player = pm.getPlayer(id);
            if (player != null) {
                ic.restoreInventory(player);
                tc.tpPlayerBack(player);
            }
        }

        // ============================================================
        // GET DIMENSION
        // ============================================================
        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        if (skirmishDim == null) {
            return;
        }

        // ============================================================
        // DELETE SKIRMISH SHIPS
        // ============================================================
        List<Ship> ships = VSGameUtilsKt.getAllShips(skirmishDim).stream().toList();
        for (Ship ship : ships) {
            if (ship instanceof ServerShip serverShip) {
                long id = serverShip.getId();
                if (id == currentSkirmish.getChShipId() || id == currentSkirmish.getOppShipId()) {
                    VLibAPI.discardShip(serverShip, skirmishDim);
                }
            }
        }

        // ============================================================
        // WIPE DIMENSION REGION CONTENTS
        // ============================================================

        skirmishDim.iterateEntities().forEach(entity -> {
            if (!(entity instanceof PlayerEntity)) {
                entity.discard();
            }
        });

        Path dimPath = server.getSavePath(WorldSavePath.ROOT)
                .resolve("dimensions")
                .resolve("vs-skirmish")
                .resolve("skirmish_dim")
                .resolve("region");

        if (Files.exists(dimPath)) {
            try (Stream<Path> stream = Files.walk(dimPath)) {
                stream.filter(p -> p.toString().endsWith(".mca"))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                VSSkirmish.LOGGER.error("[SKIRMISH] Failed to delete {}", path, e);
                            }
                        });
            } catch (IOException e) {
                VSSkirmish.LOGGER.error("[SKIRMISH] IOException during dimension wipe", e);
            }
        }

        tickCounter = 0;
        countdownTicks = 0;
        currentSkirmish = null;
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
        if (currentSkirmish != null) {
            currentSkirmish.handlePlayerQuit(server, player);
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
