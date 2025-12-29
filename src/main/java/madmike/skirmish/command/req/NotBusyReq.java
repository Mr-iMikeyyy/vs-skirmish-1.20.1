package madmike.skirmish.command.req;

import madmike.cc.logic.BusyPlayers;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class NotBusyReq {

    public static boolean require(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player != null) {
            return !BusyPlayers.isBusy(player.getUuid());
        }
        return false;
    }
}
