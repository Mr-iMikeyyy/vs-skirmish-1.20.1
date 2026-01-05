package madmike.skirmish.logic;

import madmike.cc.logic.BusyPlayers;
import madmike.cc.logic.Reason;
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
    private final StructureTemplate chShipTemplate;

    private final UUID oppPartyId;
    private final StructureTemplate oppShipTemplate;

    private final int wager;
    private final long expiresAt;

    public SkirmishChallenge(UUID chPartyId, StructureTemplate chShipTemplate, UUID oppPartyId, StructureTemplate oppShip, int wager) {
        this.chPartyId = chPartyId;
        this.chShipTemplate = chShipTemplate;

        this.oppPartyId = oppPartyId;
        this.oppShipTemplate = oppShip;

        this.wager = wager;
        this.expiresAt = (SkirmishConfig.skirmishChallengeMaxTime * 1000L) + System.currentTimeMillis();
    }

    public ServerPlayerEntity getChLeader(MinecraftServer server) {
        IServerPartyAPI party = OpenPACServerAPI.get(server).getPartyManager().getPartyById(chPartyId);
        if (party == null) {
            return null;
        }
        return server.getPlayerManager().getPlayer(party.getOwner().getUUID());
    }

    public ServerPlayerEntity getOppLeader(MinecraftServer server) {
        IServerPartyAPI party = OpenPACServerAPI.get(server).getPartyManager().getPartyById(oppPartyId);
        if (party == null) {
            return null;
        }
        return server.getPlayerManager().getPlayer(party.getOwner().getUUID());
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

    public void end(MinecraftServer server, String msg) {
        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IServerPartyAPI chParty = pm.getPartyById(chPartyId);
        IServerPartyAPI oppParty = pm.getPartyById(oppPartyId);
        if (chParty != null) {
            chParty.getOnlineMemberStream().forEach(p -> {
                if (msg != null) {
                    p.sendMessage(Text.literal(msg));
                }
                BusyPlayers.remove(p.getUuid(), Reason.SKIRMISH);
            });
        }
        if (oppParty != null) {
            oppParty.getOnlineMemberStream().forEach(p -> {
                if (msg != null) {
                    p.sendMessage(Text.literal(msg));
                }
                BusyPlayers.remove(p.getUuid(), Reason.SKIRMISH);
            });
        }
        SkirmishManager.INSTANCE.setCurrentChallenge(null);
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
