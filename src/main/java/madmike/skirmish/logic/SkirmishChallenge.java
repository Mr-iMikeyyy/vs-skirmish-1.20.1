package madmike.skirmish.logic;

import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.config.SkirmishConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.UUID;

public class SkirmishChallenge {
    UUID chPartyId;
    UUID chLeaderId;
    StructureTemplate chShip;

    UUID oppPartyId;
    UUID oppLeaderId;
    StructureTemplate oppShip;

    long wager;
    long expiresAt;

    public SkirmishChallenge(UUID chPartyId, UUID chLeaderId, StructureTemplate chShip, UUID oppPartyId, UUID oppLeaderId, StructureTemplate oppShip, long wager) {
        this.chPartyId = chPartyId;
        this.chLeaderId = chLeaderId;
        this.chShip = chShip;

        this.oppPartyId = oppPartyId;
        this.oppLeaderId = oppLeaderId;
        this.oppShip = oppShip;

        this.wager = wager;
        this.expiresAt = (SkirmishConfig.skirmishChallengeMaxTime * 1000L) + System.currentTimeMillis();
    }

    public UUID getChPartyId() {
        return chPartyId;
    }
    
    public StructureTemplate getChShip() { return chShip; }

    public UUID getOppPartyId() {
        return oppPartyId;
    }

    public long getWager() {
        return wager;
    }

    public boolean isExpired() {
        return expiresAt < System.currentTimeMillis();
    }

    public void onExpire(MinecraftServer server) {
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI chParty = pm.getPartyById(chPartyId);
        IServerPartyAPI oppParty = pm.getPartyById(oppPartyId);
        Text msg = Text.literal("The skirmish challenge has expired");
        if (chParty != null) {
            chParty.getOnlineMemberStream().forEach(p -> p.sendMessage(msg));
        }
        if (oppParty != null) {
            oppParty.getOnlineMemberStream().forEach(p -> p.sendMessage(msg));
        }
        SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundPlayer(server.getPlayerManager(), chLeaderId, wager);
    }

    public void handlePlayerQuit(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (chLeaderId.equals(id) || oppPartyId.equals(id)) {

        }
    }
}
