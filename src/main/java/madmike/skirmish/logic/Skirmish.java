package madmike.skirmish.logic;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import g_mungus.vlib.v2.api.VLibAPI;
import madmike.cc.component.CCComponents;
import madmike.cc.component.components.InventoryComponent;
import madmike.cc.component.components.TeleportComponent;
import madmike.cc.logic.BusyPlayers;
import madmike.skirmish.config.SkirmishConfig;
import madmike.skirmish.dimension.SkirmishDimension;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.chunk.ChunkStatus;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.ServerShip;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Skirmish {

    private final UUID id;
    private final long expiresAt;

    private final Set<UUID> challengers = new HashSet<>();
    private UUID chPartyId;
    private UUID chLeaderId;
    private final long chShipId;

    private final Set<UUID> opponents = new HashSet<>();
    private UUID oppPartyId;
    private UUID oppLeaderId;
    private final long oppShipId;

    private int wager;

    private final Set<UUID> spectators;

    public Skirmish() {
        this.id = UUID.randomUUID();
        this.expiresAt = (SkirmishConfig.skirmishMaxTime * 1000L) + System.currentTimeMillis();
        this.spectators = new HashSet<>();
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

        // ============================================================
        // PLACE OPPONENT SHIP
        // ============================================================
        BlockPos oppShipPos = new BlockPos(0, 31, 100);
        ServerShip oppShip = VLibAPI.placeTemplateAsShip(challenge.getOppShipTemplate(), skirmishDim, oppShipPos, false);

        if (oppShip == null) {
            challenge.end(server, "Couldn't load opponents ship");
            return false;
        }

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

    public UUID getId() { return id; }

    public UUID getOppPartyId() {
        return oppPartyId;
    }

    public UUID getChPartyId() {
        return chPartyId;
    }

    public UUID getOppLeaderId() {
        return oppLeaderId;
    }

    public UUID getChLeaderId() {
        return chLeaderId;
    }

    public Set<UUID> getAllInvolvedPlayers() {
        Set<UUID> players = new HashSet<>();
        players.addAll(spectators);
        players.addAll(opponents);
        players.addAll(challengers);
        return players;
    }

    public int getWager() {
        return wager;
    }

    public boolean isExpired() {
        return expiresAt < System.currentTimeMillis();
    }

    public boolean isPlayerInSkirmish(UUID playerId) {
        return challengers.contains(playerId) || opponents.contains(playerId);
    }

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
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.OPPONENTS_WIN_KILLS);
            }
            if (opponents.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN_KILLS);
            }
            return false;
        }

        // player not in skirmish, return true to allow death
        return true;
    }

    public boolean isShipInSkirmish(long id) {
        return id == chShipId || id == oppShipId;
    }

    public boolean isChallengerShip(long id) {
        return chShipId == id;
    }

    public void handlePlayerQuit(MinecraftServer server, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (challengers.remove(id)) {
            if (challengers.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(server, EndOfSkirmishType.OPPONENTS_WIN_KILLS);
            }
        }

        if (opponents.remove(id)) {
            if (opponents.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(server, EndOfSkirmishType.CHALLENGERS_WIN_KILLS);
            }
        }
    }

    public void addSpectator(UUID playerId) {
         spectators.add(playerId);
    }

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

    public long getChShipId() {
        return chShipId;
    }

    public long getOppShipId() {
        return oppShipId;
    }

    public Set<UUID> getStillAlive() {
        Set<UUID> stillAlive = new HashSet<>();
        stillAlive.addAll(challengers);
        stillAlive.addAll(opponents);
        return stillAlive;
    }

    public boolean isPlayerSpectating(UUID id) {
        return spectators.contains(id);
    }

    public void removeSpectator(MinecraftServer server, ServerPlayerEntity player) {
        CCComponents.TP.get(server.getScoreboard()).tpPlayerBack(player);
        spectators.remove(player.getUuid());
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
