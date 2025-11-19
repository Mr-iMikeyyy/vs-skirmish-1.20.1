package madmike.skirmish.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

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
            if (currentSkirmish.getExpiredTime() <= System.currentTimeMillis()) {
                endSkirmish(server, EndOfSkirmishType.TIME);
            }
        }
        if (currentChallenge != null) {
            if (currentChallenge.expiresAt >= System.currentTimeMillis()) {
                currentChallenge = null;
            }
        }
    }

    public void startSkirmish(StructureTemplate oppShip) {

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
            currentSkirmish.handlePlayerQuit(server, player);
        }
    }

    public void addSpectator(ServerPlayerEntity player) {
        
    }
}
