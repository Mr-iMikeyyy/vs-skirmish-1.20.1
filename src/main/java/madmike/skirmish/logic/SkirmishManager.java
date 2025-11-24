package madmike.skirmish.logic;

import g_mungus.vlib.v2.api.VLibAPI;
import g_mungus.vlib.v2.api.extension.ShipExtKt;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.component.components.InventoryComponent;
import madmike.skirmish.component.components.ReturnPointComponent;
import madmike.skirmish.dimension.SkirmishDimension;
import madmike.skirmish.feature.blocks.SkirmishSpawnBlock;
import net.minecraft.block.BlockState;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.io.IOException;
import java.nio.file.FileVisitOption;
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
        }
        if (currentChallenge != null) {
            if (currentChallenge.isExpired()) {
                currentChallenge.onExpire(server);
                currentChallenge = null;
            }
        }
    }

    public void startSkirmish(MinecraftServer server, SkirmishChallenge challenge) {
        //spawn ships
        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        if (skirmishDim == null) {
            return;
        }

        // CH SHIP

        ServerShip chShip = VLibAPI.placeTemplateAsShip(challenge.getChShipTemplate(), skirmishDim, new BlockPos(0, 30, 0), false);
        if (chShip == null) {
            challenge.broadcastMsg(server, "Error getting/placing challengers ship, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        // Scan for a Skirmish Spawn Block
        AtomicBoolean found = new AtomicBoolean(false);
        BlockPos[] foundPos = new BlockPos[1]; // mutable container

        ShipExtKt.forEachBlock(chShip, blockPos -> {
            if (found.get()) return null;

            BlockState state = skirmishDim.getBlockState(blockPos);

            if (state.getBlock() instanceof SkirmishSpawnBlock) {
                found.set(true);
                foundPos[0] = blockPos;
            }
            return null;
        });

        if (foundPos[0] == null) {
            challenge.broadcastMsg(server,"Could not detect a Skirmish Spawn Block on challenger ship, this should never happen, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        // OPP SHIP

        ServerShip oppShip = VLibAPI.placeTemplateAsShip(challenge.getOppShipTemplate(), skirmishDim, new BlockPos(0, 30, 0), false);
        if (oppShip == null) {
            challenge.broadcastMsg(server, "Error getting/placing opponents ship, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        // Scan for a Skirmish Spawn Block
        AtomicBoolean found2 = new AtomicBoolean(false);
        BlockPos[] foundPos2 = new BlockPos[1]; // mutable container

        ShipExtKt.forEachBlock(oppShip, blockPos -> {
            if (found2.get()) return null;

            BlockState state = skirmishDim.getBlockState(blockPos);

            if (state.getBlock() instanceof SkirmishSpawnBlock) {
                found2.set(true);
                foundPos2[0] = blockPos;
            }
            return null;
        });

        if (foundPos2[0] == null) {
            challenge.broadcastMsg(server,"Could not detect a Skirmish Spawn Block on opponent ship, this should never happen, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        // PLAYERS
        // gather and teleport players

        Scoreboard sb = server.getScoreboard();
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();

        IServerPartyAPI chParty = pm.getPartyById(challenge.getChPartyId());
        if (chParty == null) {
            challenge.broadcastMsg(server,"Error getting challenging party, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        IServerPartyAPI oppParty = pm.getPartyById(challenge.getOppPartyId());
        if (oppParty == null) {
            challenge.broadcastMsg(server,"Error getting opponent party, cancelling skirmish");
            SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundChallenge(server, challenge);
            currentChallenge = null;
            return;
        }

        Set<UUID> challengerIds = new HashSet<>();

        chParty.getOnlineMemberStream().forEach(player -> {
            SkirmishComponents.RETURN_POINTS.get(sb).set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());
            SkirmishComponents.INVENTORY.get(sb).saveInventory(player);
            challengerIds.add(player.getUuid());
            player.teleport(skirmishDim, foundPos[0].getX(), foundPos[0].getY(), foundPos[0].getZ(), player.getYaw(), player.getPitch());
        });

        Set<UUID> opponentIds = new HashSet<>();

        oppParty.getOnlineMemberStream().forEach(player -> {
            SkirmishComponents.RETURN_POINTS.get(sb).set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());
            SkirmishComponents.INVENTORY.get(sb).saveInventory(player);
            opponentIds.add(player.getUuid());
            player.teleport(skirmishDim, foundPos2[0].getX(), foundPos2[0].getY(), foundPos2[0].getZ(), player.getYaw(), player.getPitch());
        });

        //create skirmish
        currentSkirmish = new Skirmish(challengerIds, chParty.getId(), opponentIds, oppParty.getId(), chShip.getId(), oppShip.getId());
        //broadcast msg to all players
        challenge.broadcastMsg(server, "Skirmish Started!");
        server.getPlayerManager().getPlayerList().forEach( player -> {
            UUID playerId = player.getUuid();
            if (!challengerIds.contains(playerId) && !opponentIds.contains(playerId)) {
                player.sendMessage(Text.literal("A skirmish has started! Use /skirmish spectate to watch!"));
            }
        });
        currentChallenge = null;
    }

    public void endSkirmish(MinecraftServer server, EndOfSkirmishType type) {
        ServerWorld skirmishDim = server.getWorld(SkirmishDimension.SKIRMISH_LEVEL_KEY);
        if (skirmishDim == null) {
            return;
        }

        // award winner
        // record stats
        StatsComponent sc = SkirmishComponents.STATS.get(server.getScoreboard());

        switch (type) {
            case CHALLENGERS_WIN_KILLS -> {

            }
            case OPPONENTS_WIN_KILLS -> {
            }
            case CHALLENGERS_WIN_SHIP -> {
            }
            case OPPONENTS_WIN_SHIP -> {
            }
            case TIME -> {
            }
        }

        // tp back
        // inventories
        // game mode
        Set<UUID> players = currentSkirmish.getAllInvolvedPlayers();
        PlayerManager pm = server.getPlayerManager();
        InventoryComponent ic = SkirmishComponents.INVENTORY.get(server.getScoreboard());
        ReturnPointComponent rc = SkirmishComponents.RETURN_POINTS.get(server.getScoreboard());
        for (UUID id : players) {
            ServerPlayerEntity player = pm.getPlayer(id);
            if (player != null) {
                player.changeGameMode(GameMode.SURVIVAL);
                ic.restoreInventory(player);
                rc.tpBack(server, player);
            }
        }


        //kill ships
        List<Ship> ships = VSGameUtilsKt.getAllShips(skirmishDim).stream().toList();
        for (Ship ship : ships) {
            if (ship instanceof ServerShip serverShip) {
                VLibAPI.discardShip(serverShip, skirmishDim);
            }
        }


        // wipe dimension
        Path dimPath = server.getSavePath(WorldSavePath.ROOT)
                .resolve("dimensions")
                .resolve("vs-skirmish")
                .resolve("region");

        if (!Files.exists(dimPath)) {
            System.out.println("Skirmish region folder does not exist: " + dimPath);
            return;
        }

        try (Stream<Path> stream = Files.walk(dimPath)) {
            stream.filter(p -> p.toString().endsWith(".mca"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            System.out.println("Deleted: " + path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path);
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
