package madmike.skirmish.logic;

import madmike.skirmish.config.SkirmishConfig;
import madmike.skirmish.dimension.SkirmishDimension;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureTemplate;
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

    private final Set<UUID> challengers;

    private final Set<UUID> opponents;

    private Set<UUID> spectators;

    long chShipId;

    long oppShipId;

    private final long expiresAt;

    public Skirmish(Set<UUID> challengers, Set<UUID> opponents, long chShipId, long oppShipId) {
        this.challengers = challengers;
        this.opponents = opponents;
        this.chShipId = chShipId;
        this.oppShipId = oppShipId;
        this.expiresAt = (SkirmishConfig.skirmishMaxTime * 1000L) + System.currentTimeMillis();
    }

    public boolean isExpired() {
        return expiresAt < System.currentTimeMillis();
    }

    public boolean isPlayerInSkirmish(ServerPlayerEntity player) {
        return challengers.contains(player.getUuid()) || opponents.contains(player.getUuid());
    }

    public boolean handlePlayerDeath(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (challengers.remove(id)) {
            player.setHealth(player.getMaxHealth());
            if (challengers.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.OPPONENTS_WIN);
            }
            else {
                player.changeGameMode(GameMode.SPECTATOR);
                spectators.add(id);
                player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));
            }
            return false;
        }

        if (opponents.remove(id)) {
            player.setHealth(player.getMaxHealth());
            if (opponents.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN);
            }
            else {
                player.changeGameMode(GameMode.SPECTATOR);
                spectators.add(id);
                player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));
            }
            return false;
        }

        // player not in a skirmish, return true to allow death
        return true;
    }

    public boolean isShipInSkirmish(long id) {
        return id == chShipId || id == oppShipId;
    }

    public boolean isChallengerShip(long id) {
        return chShipId == id;
    }

    public void handlePlayerQuit(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (challengers.remove(id)) {
            if (challengers.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN);
            }
            else {
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));
            }
        }

        if (opponents.remove(id)) {
            player.setHealth(player.getMaxHealth());
            if (opponents.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN);
            }
            else {
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));
            }
        }
    }

    public void addSpectator(MinecraftServer server, ServerPlayerEntity player) {
         PlayerManager pm = server.getPlayerManager();
         BlockPos found = null;
         Set<UUID> allIds = new HashSet<>();
         allIds.addAll(challengers);
         allIds.addAll(opponents);
         for (UUID id : allIds) {
             ServerPlayerEntity skirmisher = pm.getPlayer(id);
             if (skirmisher != null) {
                 found = skirmisher.getBlockPos();
                 break;
             }
         }
         if (found != null) {
             player.teleport(server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY), found.getX(), found.getY(), found.getZ(), player.getYaw(), player.getPitch());
             player.changeGameMode(GameMode.SPECTATOR);
             spectators.add(player.getUuid());
         }
         else {
             player.sendMessage(Text.literal("Error trying to spectate"));
         }
    }
}
