package madmike.skirmish.command.req;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class PartyLeaderReq {
    public static boolean reqPartyLeader(ServerCommandSource src) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return false;
        IServerPartyAPI party = OpenPACServerAPI.get(src.getServer()).getPartyManager().getPartyByOwner(player.getUuid());
        return party != null;
    }
}
