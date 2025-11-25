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
    private final UUID chPartyId;
    private final UUID chLeaderId;
    private final StructureTemplate chShipTemplate;

    private final UUID oppPartyId;
    private final UUID oppLeaderId;
    private final StructureTemplate oppShipTemplate;

    private final int wager;
    private final long expiresAt;

    public SkirmishChallenge(UUID chPartyId, UUID chLeaderId, StructureTemplate chShipTemplate, UUID oppPartyId, UUID oppLeaderId, StructureTemplate oppShip, int wager) {
        this.chPartyId = chPartyId;
        this.chLeaderId = chLeaderId;
        this.chShipTemplate = chShipTemplate;

        this.oppPartyId = oppPartyId;
        this.oppLeaderId = oppLeaderId;
        this.oppShipTemplate = oppShip;

        this.wager = wager;
        this.expiresAt = (SkirmishConfig.skirmishChallengeMaxTime * 1000L) + System.currentTimeMillis();
    }

    public UUID getChLeaderId() {
        return chLeaderId;
    }

    public UUID getOppLeaderId() {
        return oppLeaderId;
    }

    public UUID getChPartyId() {
        return chPartyId;
    }
    
    public StructureTemplate getChShipTemplate() { return chShipTemplate; }

    public UUID getOppPartyId() {
        return oppPartyId;
    }

    public StructureTemplate getOppShipTemplate() { return oppShipTemplate; }

    public int getWager() {
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
        SkirmishComponents.REFUNDS.get(server.getScoreboard()).refundPlayer(server, chLeaderId, wager);
    }

    public void handlePlayerQuit(MinecraftServer server, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (chLeaderId.equals(id) || oppLeaderId.equals(id)) {
            broadcastMsg(player.getServer(), "One of the party leaders has quit, cancelling skirmish challenge");
            SkirmishComponents.REFUNDS.get(player.getScoreboard()).refundPlayer(server, chLeaderId, wager);
        }
    }

    public void broadcastMsg(MinecraftServer server, String msg) {
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI chParty = pm.getPartyById(chPartyId);
        if (chParty != null) {
            chParty.getOnlineMemberStream().forEach(p -> {
                p.sendMessage(Text.literal(msg));
            });
        }
        IServerPartyAPI oppParty = pm.getPartyById(oppPartyId);
        if (oppParty != null) {
            oppParty.getOnlineMemberStream().forEach(p -> {
                p.sendMessage(Text.literal(msg));
            });
        }
    }


}
