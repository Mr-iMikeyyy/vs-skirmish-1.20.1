package madmike.skirmish.command.req;

import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.command.ServerCommandSource;

public class SkirmishOngoingReq {
    public static boolean require(ServerCommandSource source) {
        return SkirmishManager.INSTANCE.getCurrentSkirmish() != null;
    }
}
