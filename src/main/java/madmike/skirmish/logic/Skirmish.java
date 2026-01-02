package madmike.skirmish.logic;

import madmike.cc.component.CCComponents;
import madmike.cc.logic.BusyPlayers;
import madmike.skirmish.config.SkirmishConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Skirmish {

    private final UUID id;

    private final Set<UUID> challengers;
    private final UUID chPartyId;
    private final UUID chLeaderId;

    private final Set<UUID> opponents;
    private final UUID oppPartyId;
    private final UUID oppLeaderId;

    private final Set<UUID> spectators;

    private final long chShipId;

    private final long oppShipId;

    private final int wager;

    private final long expiresAt;

    public Skirmish(Set<UUID> challengers, UUID chPartyId, UUID chLeaderId, Set<UUID> opponents, UUID oppPartyId, UUID oppLeaderId, long chShipId, long oppShipId, int wager) {
        this.id = UUID.randomUUID();
        this.challengers = challengers;
        this.chPartyId = chPartyId;
        this.chLeaderId = chLeaderId;
        this.opponents = opponents;
        this.oppPartyId = oppPartyId;
        this.oppLeaderId = oppLeaderId;
        this.chShipId = chShipId;
        this.oppShipId = oppShipId;
        this.wager = wager;
        this.expiresAt = (SkirmishConfig.skirmishMaxTime * 1000L) + System.currentTimeMillis();
        this.spectators = new HashSet<>();
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
