package madmike.skirmish.command.req;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xaero.pac.common.server.api.OpenPACServerAPI;

public class PartyLeaderReq {
    public static boolean require(ServerCommandSource source) {

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return false;
        }

        return OpenPACServerAPI.get(source.getServer()).getPartyManager().getPartyByOwner(player.getUuid()) != null;
    }
}
