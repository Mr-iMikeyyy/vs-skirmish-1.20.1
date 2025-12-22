package madmike.skirmish.command.req;

import madmike.duels.logic.Duel;
import madmike.duels.logic.DuelManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class NotInDuelReq {
    public static boolean require(ServerCommandSource src) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            return false;
        }
        if (FabricLoader.getInstance().isModLoaded("duels")) {
            Duel duel = DuelManager.INSTANCE.getDuelByPlayer(player.getUuid());
            return duel == null;
        }
        return true;
    }
}
