package madmike.skirmish.logic;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import g_mungus.vlib.v2.api.VLibAPI;
import madmike.cc.component.CCComponents;
import madmike.cc.component.components.BankComponent;
import madmike.cc.component.components.InventoryComponent;
import madmike.cc.component.components.TeleportComponent;
import madmike.cc.logic.BusyPlayers;
import madmike.cc.logic.Reason;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.component.components.StatsComponent;
import madmike.skirmish.config.SkirmishConfig;
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
import java.util.stream.Stream;

public class Skirmish {

    private final UUID id;
    private final long expiresAt;

    private final Set<UUID> challengers = new HashSet<>();
    private UUID chPartyId;
    private UUID chLeaderId;
    private long chShipId;

    private final Set<UUID> opponents = new HashSet<>();
    private UUID oppPartyId;
    private UUID oppLeaderId;
    private long oppShipId;

    private int wager;

    private int countdownTicks = 200;

    private boolean isCountingdown = true;

    private final Set<UUID> spectators = new HashSet<>();

    public Skirmish() {
        this.id = UUID.randomUUID();
        this.expiresAt = (SkirmishConfig.skirmishMaxTime * 1000L) + System.currentTimeMillis();
    }

    /* ---------- start ---------- */
    public boolean start(MinecraftServer server, SkirmishChallenge challenge) {
        this.wager = challenge.getWager();
        this.chPartyId = challenge.getChPartyId();
        this.oppPartyId = challenge.getOppPartyId();

        // ============================================================
        // CHECK LEADERS
        // ============================================================

        ServerPlayerEntity chLeader = challenge.getChLeader(server);
        ServerPlayerEntity oppLeader = challenge.getOppLeader(server);
        if (chLeader == null || oppLeader == null) {
            challenge.end(server, "Couldn't get one of the party leaders, cancelling match.");
            return false;
        }
        this.chLeaderId = chLeader.getUuid();
        this.oppLeaderId = oppLeader.getUuid();

        // ============================================================
        // CHECK WAGER
        // ============================================================

        CurrencyComponent chWallet = ModComponents.CURRENCY.get(chLeader);
        CurrencyComponent oppWallet = ModComponents.CURRENCY.get(oppLeader);
        long bet = wager * 10000L;
        if (chWallet.getValue() < bet) {
            challenge.end(server, "Challenger didn't have enough gold in their wallet");
            return false;
        }
        if (oppWallet.getValue() < bet) {
            challenge.end(server, "Opponent didn't have enough gold in their wallet");
            return false;
        }

        // ============================================================
        // LOAD SKIRMISH DIMENSION
        // ============================================================
        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);

        if (skirmishDim == null) {
            challenge.end(server, "Couldn't load skirmish dimension");
            return false;
        }

        // ============================================================
        // PLACE CHALLENGER SHIP
        // ============================================================

        BlockPos chShipPos = new BlockPos(0, 31, -100);
        ServerShip chShip = VLibAPI.placeTemplateAsShip(challenge.getChShipTemplate(), skirmishDim, chShipPos, false);

        if (chShip == null) {
            challenge.end(server, "Couldn't load challengers ship");
            return false;
        }

        chShipId = chShip.getId();

        // ============================================================
        // PLACE OPPONENT SHIP
        // ============================================================
        BlockPos oppShipPos = new BlockPos(0, 31, 100);
        ServerShip oppShip = VLibAPI.placeTemplateAsShip(challenge.getOppShipTemplate(), skirmishDim, oppShipPos, false);

        if (oppShip == null) {
            challenge.end(server, "Couldn't load opponents ship");
            return false;
        }

        oppShipId = oppShip.getId();

        // ============================================================
        // LOAD PARTIES
        // ============================================================

        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();

        IServerPartyAPI chParty = pm.getPartyById(challenge.getChPartyId());
        if (chParty == null) {
            challenge.end(server, "Couldn't load challenging party");
            return false;
        }

        IServerPartyAPI oppParty = pm.getPartyById(challenge.getOppPartyId());
        if (oppParty == null) {
            challenge.end(server, "Couldn't load opponent party");
            return false;
        }

        // ============================================================
        // TP PLAYERS AND SAVE INV
        // ============================================================

        Scoreboard sb = server.getScoreboard();

        TeleportComponent tp = CCComponents.TP.get(sb);
        InventoryComponent inv = CCComponents.INV.get(sb);

        AABBdc chShipWorldAABB = chShip.getWorldAABB();
        double chLength = chShipWorldAABB.maxZ() - chShipWorldAABB.minZ();
        double chSafe = chLength / 2.0 + 10.0; // 10 blocks of air buffer
        Vector3d chShipCenter = chShipWorldAABB.center(new Vector3d());
        BlockPos chSpawn = new BlockPos(0, 64, (int) (chShipCenter.z - chSafe));
        skirmishDim.getChunk(chSpawn.getX() >> 4, chSpawn.getZ() >> 4, ChunkStatus.FULL, true);

        chParty.getOnlineMemberStream().forEach(player -> {
            player.setNoGravity(true);
            challengers.add(player.getUuid());

            tp.set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());

            inv.saveInventory(player);

            player.teleport(skirmishDim, chSpawn.getX(), chSpawn.getY(), chSpawn.getZ(), player.getYaw(), player.getPitch());
        });

        AABBdc oppShipWorldAABB = oppShip.getWorldAABB();
        double oppLength = oppShipWorldAABB.maxZ() - oppShipWorldAABB.minZ();
        double oppSafe = oppLength / 2.0 + 10.0; // 10 blocks of air buffer
        Vector3d oppShipCenter = oppShipWorldAABB.center(new Vector3d());
        BlockPos oppSpawn = new BlockPos(0, 64, (int) (oppShipCenter.z + oppSafe));
        skirmishDim.getChunk(oppSpawn.getX() >> 4, oppSpawn.getZ() >> 4, ChunkStatus.FULL, true);

        oppParty.getOnlineMemberStream().forEach(player -> {
            player.setNoGravity(true);
            opponents.add(player.getUuid());

            tp.set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());

            inv.saveInventory(player);

            player.teleport(skirmishDim, oppSpawn.getX(), oppSpawn.getY(), oppSpawn.getZ(), player.getYaw(), player.getPitch());
        });

        // ============================================================
        // TAKE MONEY
        // ============================================================
        chWallet.modify(-bet);
        oppWallet.modify(-bet);

        // ============================================================
        // BROADCAST
        // ============================================================

        challenge.broadcastMsg(server, "Skirmish Starting!");

        server.getPlayerManager().getPlayerList().forEach(player -> {
            UUID playerId = player.getUuid();
            if (!challengers.contains(playerId) && !opponents.contains(playerId)) {
                player.sendMessage(Text.literal("A skirmish has started! Use /skirmish spectate to watch!"));
            }
        });

        return true;
    }

    /* ---------- count down ---------- */
    public boolean hasCountdown() {
        return isCountingdown;
    }
    public void countDownTick(MinecraftServer server) {
        countdownTicks--;
        if (countdownTicks % 20 == 0) {
            int secondsLeft = countdownTicks / 20;
            if (secondsLeft != 0) {
                broadcastMsg(server, "§eSkirmish starts in §c" + secondsLeft);
            }
            else {
                isCountingdown = false;
                broadcastMsg(server, "§eSkirmish has started! Fight!");
                PlayerManager pm = server.getPlayerManager();
                for (UUID id : getAllInvolvedPlayers()) {
                    ServerPlayerEntity player = pm.getPlayer(id);
                    if (player != null) {
                        player.setNoGravity(false);
                    }
                }
            }
        }
    }

    /* ================= expire ================= */
    public boolean isExpired() {
        return expiresAt < System.currentTimeMillis();
    }

    /* ---------- end ---------- */
    public void end(MinecraftServer server, EndOfSkirmishType type) {
        // ============================================================
        // TELEPORT PLAYERS BACK & RESTORE INVENTORY
        // ============================================================

        flushPlayers(server);

        // ============================================================
        // RECORD STATS AND REWARD PLAYERS
        // ============================================================

        Scoreboard sb = server.getScoreboard();
        BankComponent bank = CCComponents.BANK.get(sb);
        StatsComponent stats = SkirmishComponents.STATS.get(sb);
        switch (type) {

            case CHALLENGERS_WIN_KILLS -> {

                stats.setPartySkirmishStats(chPartyId, 1, 0,
                        wager, 0, 0, 0);

                stats.setPartySkirmishStats(oppPartyId, 0, 1,
                        0, wager, 0, 0);

                bank.reward(id, chLeaderId);

                broadcastMsg(server, "Challengers won by kills!");
            }

            case OPPONENTS_WIN_KILLS -> {

                stats.setPartySkirmishStats(chPartyId, 0, 1,
                        0, wager, 0, 0);

                stats.setPartySkirmishStats(oppPartyId, 1, 0,
                        wager, 0, 0, 0);

                bank.reward(id, oppLeaderId);

                broadcastMsg(server, "Opponents won by kills!");
            }

            case CHALLENGERS_WIN_SHIP -> {

                stats.setPartySkirmishStats(chPartyId, 1, 0,
                        wager, 0, 0, 1);

                stats.setPartySkirmishStats(oppPartyId, 0, 1,
                        0, wager, 1, 0);

                bank.reward(id, chLeaderId);

                broadcastMsg(server, "Challengers won by sinking the ship!");
            }

            case OPPONENTS_WIN_SHIP -> {

                stats.setPartySkirmishStats(chPartyId, 0, 1,
                        0, wager, 1, 0);

                stats.setPartySkirmishStats(oppPartyId, 1, 0,
                        wager, 0, 0, 1);

                bank.reward(id, oppLeaderId);

                broadcastMsg(server, "Opponents won by sinking the ship!");
            }

            case TIME -> {

                bank.reward(id, null);

                broadcastMsg(server, "The time ran out on the skirmish!");
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
                if (id == chShipId || id == oppShipId) {
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

        SkirmishManager.INSTANCE.resetMatch();
    }
    private void flushPlayers(MinecraftServer server) {
        Set<UUID> players = getAllInvolvedPlayers();

        PlayerManager pm = server.getPlayerManager();
        Scoreboard sb = server.getScoreboard();
        TeleportComponent tp = CCComponents.TP.get(sb);
        InventoryComponent ic = CCComponents.INV.get(sb);

        for (UUID id : players) {
            ServerPlayerEntity player = pm.getPlayer(id);
            if (player != null) {
                tp.tpPlayerBack(player, Reason.TDM);
                ic.restoreInventory(player);
            }
        }
    }

    /* ---------- events ---------- */
    public boolean handlePlayerDeath(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (challengers.remove(id) || opponents.remove(id)) {

            player.setHealth(player.getMaxHealth());

            player.changeGameMode(GameMode.SPECTATOR);
            player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));
            spectators.add(id);

            BlockPos currentPos = player.getBlockPos();
            if (currentPos.getY() < 30) {
                player.teleport(player.getServerWorld(), currentPos.getX(), 30, currentPos.getZ(), player.getYaw(), player.getPitch());
            }

            if (challengers.isEmpty()) {
                end(player.getServer(), EndOfSkirmishType.OPPONENTS_WIN_KILLS);
            }
            if (opponents.isEmpty()) {
                end(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN_KILLS);
            }
            return false;
        }

        // player not in skirmish, return true to allow death
        return true;
    }
    public void handlePlayerQuit(MinecraftServer server, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (challengers.remove(id)) {
            if (challengers.isEmpty()) {
                end(server, EndOfSkirmishType.OPPONENTS_WIN_KILLS);
            }
        }

        if (opponents.remove(id)) {
            if (opponents.isEmpty()) {
                end(server, EndOfSkirmishType.CHALLENGERS_WIN_KILLS);
            }
        }
    }

    /* ---------- ships ---------- */
    public boolean isShipInSkirmish(long id) {
        return id == chShipId || id == oppShipId;
    }
    public boolean isChallengerShip(long id) {
        return chShipId == id;
    }

    /* ---------- spectating ---------- */
    public void addSpectator(MinecraftServer server, ServerPlayerEntity spec) {
        ServerPlayerEntity target = null;
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(challengers);
        allPlayers.addAll(opponents);
        PlayerManager pm = server.getPlayerManager();
        for (UUID id : allPlayers) {
            ServerPlayerEntity player = pm.getPlayer(id);
            if (player != null) {
                target = player;
                break;
            }
        }

        if (target == null) {
            spec.sendMessage(Text.literal("Error finding teleport pos"));
            return;
        }

        CCComponents.TP.get(server.getScoreboard()).set(spec.getUuid(), spec.getBlockPos(), spec.getServerWorld().getRegistryKey());

        BlockPos targetPos = target.getBlockPos();
        spec.teleport(target.getServerWorld(), targetPos.getX(), targetPos.getY(), targetPos.getZ(), spec.getYaw(), spec.getPitch());

        spec.changeGameMode(GameMode.SPECTATOR);

        BusyPlayers.add(spec.getUuid(), Reason.SKIRMISH);

        spec.sendMessage(Text.literal("You are now spectating, enjoy!"));
        spectators.add(spec.getUuid());
    }
    public boolean isPlayerSpectating(UUID id) {
        return spectators.contains(id);
    }
    public void removeSpectator(MinecraftServer server, ServerPlayerEntity player) {
        CCComponents.TP.get(server.getScoreboard()).tpPlayerBack(player, Reason.SKIRMISH);
        spectators.remove(player.getUuid());
    }

    /* ---------- broadcast ---------- */
    public void broadcastMsg(MinecraftServer server, String msg) {
        broadcastMsgToChParty(server, msg);
        broadcastMsgToOppParty(server, msg);
    }
    public void broadcastMsgToChParty(MinecraftServer server, String msg) {
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI chParty = pm.getPartyById(chPartyId);
        if (chParty != null) {
            chParty.getOnlineMemberStream().forEach(p -> {
                p.sendMessage(Text.literal(msg));
            });
        }
    }
    public void broadcastMsgToOppParty(MinecraftServer server, String msg) {
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI oppParty = pm.getPartyById(oppPartyId);
        if (oppParty != null) {
            oppParty.getOnlineMemberStream().forEach(p -> {
                p.sendMessage(Text.literal(msg));
            });
        }
    }

    /* ---------- players ---------- */
    public Set<UUID> getAllInvolvedPlayers() {
        Set<UUID> players = new HashSet<>();
        players.addAll(spectators);
        players.addAll(opponents);
        players.addAll(challengers);
        return players;
    }
    public boolean isPlayerInSkirmish(UUID playerId) {
        return challengers.contains(playerId) || opponents.contains(playerId);
    }

//    public void runDistanceCheck(MinecraftServer server) {
//        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
//
//        ServerShipWorldCore vs = VSGameUtilsKt.getShipObjectWorld(skirmishDim);
//        ServerShip chShip = vs.getLoadedShips().stream()
//                .filter(ship -> ship.getId() == chShipId).findFirst().orElse(null);
//        ServerShip oppShip = vs.getLoadedShips().stream()
//                .filter(ship -> ship.getId() == oppShipId).findFirst().orElse(null);
//
//        if (chShip == null || oppShip == null) {
//            // Ship not loaded yet (or skirmish just ended)
//            return;
//        }
//
//        AABBdc chShipAABB = chShip.getWorldAABB();
//        AABBdc oppShipAABB = oppShip.getWorldAABB();
//        Vector3d chCenter = chShipAABB.center(new Vector3d());
//        Vector3d oppCenter = oppShipAABB.center(new Vector3d());
//        double distance = chCenter.distance(oppCenter);
//        if (distance >= 50) {
//            broadcastMsgToOppParty(server, "Challenger's Ship at " + (int) chCenter.x() + ", " + (int) chCenter.y() + ", " + (int) chCenter.z());
//            broadcastMsgToChParty(server, "Opponent's Ship at " + (int) oppCenter.x() + ", " + (int) oppCenter.y() + ", " + (int) oppCenter.z());
//        }
//    }
}
