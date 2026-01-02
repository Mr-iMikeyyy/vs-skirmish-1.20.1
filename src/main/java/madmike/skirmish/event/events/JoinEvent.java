package madmike.skirmish.event.events;

import madmike.cc.logic.BusyPlayers;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.logic.SkirmishChallenge;
import madmike.skirmish.logic.SkirmishManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.PartyManager;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class JoinEvent {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(((serverPlayNetworkHandler, packetSender, server) -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();

            IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();

            IServerPartyAPI party = pm.getPartyByMember(player.getUuid());

            if (party != null) {
                SkirmishChallenge challenge = SkirmishManager.INSTANCE.getCurrentChallenge();
                if (challenge != null) {
                    if (challenge.getChPartyId().equals(party.getId()) || challenge.getOppPartyId().equals(party.getId())) {
                        BusyPlayers.add(player.getUuid());
                    }
                }
            }

        }));
    }
}
