package madmike.skirmish.command.req;

import madmike.skirmish.logic.Skirmish;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class IsSpectatingReq {
    public static boolean require(ServerCommandSource source) {
        Skirmish skirmish = SkirmishManager.INSTANCE.getCurrentSkirmish();
        if (skirmish == null) {
            return false;
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return false;
        }
        return skirmish.isPlayerSpectating(player.getUuid());
    }
}
