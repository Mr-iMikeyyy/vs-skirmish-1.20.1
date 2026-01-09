package madmike.skirmish.event.events;

import madmike.skirmish.logic.SkirmishChallengeManager;
import madmike.skirmish.logic.SkirmishManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class EndServerTickEvent {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            SkirmishManager.INSTANCE.tick(server);
            SkirmishChallengeManager.INSTANCE.tick(server);
        });
    }
}
