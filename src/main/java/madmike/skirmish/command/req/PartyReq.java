package madmike.skirmish.command.req;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class PartyReq {
    public static boolean require(ServerCommandSource src) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return false;
        IServerPartyAPI party = OpenPACServerAPI.get(src.getServer()).getPartyManager().getPartyByMember(player.getUuid());
        return party != null;
    }
}
