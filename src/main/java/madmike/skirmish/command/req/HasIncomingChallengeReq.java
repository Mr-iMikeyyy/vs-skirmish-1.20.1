package madmike.skirmish.command.req;

import madmike.skirmish.logic.SkirmishChallengeManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class HasIncomingChallengeReq {
    public static boolean require(ServerCommandSource source) {

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return false;
        }

        IServerPartyAPI party = OpenPACServerAPI.get(source.getServer()).getPartyManager().getPartyByOwner(player.getUuid());
        if (party == null) {
            return false;
        }

        return SkirmishChallengeManager.INSTANCE.getIncomingChallengeFor(party.getId()) != null;
    }
}
