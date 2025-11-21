package madmike.skirmish.logic;

import g_mungus.vlib.api.VLibGameUtils;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.dimension.SkirmishDimension;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import org.valkyrienskies.core.api.ships.Ship;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
        StructurePlacementData data = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(BlockRotation.NONE)
                .setIgnoreEntities(false)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);

        challenge.chShip.place(skirmishDim, new BlockPos(0, 30, 0), new BlockPos(0, 30, 0), data, skirmishDim.getRandom(), 2);


        // gather and teleport players
        Scoreboard sb = server.getScoreboard();
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();

        IServerPartyAPI chParty = pm.getPartyById(challenge.chPartyId);
        if (chParty == null) {
            return;
        }

        IServerPartyAPI oppParty = pm.getPartyById(challenge.oppPartyId);
        if (oppParty == null) {
            return;
        }

        Set<UUID> challengers = new HashSet<>();


        chParty.getOnlineMemberStream().forEach(player -> {
            SkirmishComponents.RETURN_POINTS.get(sb).set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());
            SkirmishComponents.INVENTORY.get(sb).saveInventory(player);
            challengers.add(player.getUuid());
            player.teleport(skirmishDim, );
        });

        //create skirmish
        currentSkirmish = new Skirmish();
        //broadcast msg to all players
    }

    public void endSkirmish(MinecraftServer server, EndOfSkirmishType type) {
        // award winners
        // record stats
        // tp back
        // wipe dimension
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
            endSkirmish(server, EndOfSkirmishType.OPPONENTS_WIN);
        }
        else {
            endSkirmish(server, EndOfSkirmishType.CHALLENGERS_WIN);
        }
    }

    public void handlePlayerQuit(MinecraftServer server, ServerPlayerEntity player) {
        if (currentChallenge != null) {
            currentChallenge.handlePlayerQuit(player);
            IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
            IServerPartyAPI chParty = pm.getPartyById(currentChallenge.chPartyId);
            IServerPartyAPI oppParty = pm.getPartyById(currentChallenge.oppPartyId);
            //Check if player who left is party leader for either party
            if (chParty.getOwner().getUUID().equals(player.getUuid()) || oppParty.getOwner().getUUID().equals(player.getUuid())) {
                //Cancel challenge
                chParty.getOnlineMemberStream().forEach(p -> p.sendMessage(Text.literal("Skirmish challenge cancelled because a party leader left.")));
                oppParty.getOnlineMemberStream().forEach(p -> p.sendMessage(Text.literal("Skirmish challenge cancelled because a party leader left.")));
            }
        }

        if (currentSkirmish != null) {
            currentSkirmish.handlePlayerQuit(player);
        }
    }
}
