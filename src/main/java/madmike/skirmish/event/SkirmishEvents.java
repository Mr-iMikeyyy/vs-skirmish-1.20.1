package madmike.skirmish.event;

import madmike.skirmish.VSSkirmish;
import madmike.skirmish.event.events.*;

public class SkirmishEvents {
    public static void register() {
        try {
            VSSkirmish.LOG.info("Registering Skirmish events");

            AllowDeathEvent.register();
            VSSkirmish.LOG.debug("Registered AllowDeathEvent");

            DisconnectEvent.register();
            VSSkirmish.LOG.debug("Registered DisconnectEvent");

            EndServerTickEvent.register();
            VSSkirmish.LOG.debug("Registered EndServerTickEvent");

            EntitySpawnEvent.register();
            VSSkirmish.LOG.debug("Registered EntitySpawnEvent");

            JoinEvent.register();
            VSSkirmish.LOG.debug("Registered JoinEvent");

            ShipHelmBlockDestroyedEvent.register();
            VSSkirmish.LOG.debug("Registered ShipHelmBlockDestroyedEvent");

            VSSkirmish.LOG.info("All Skirmish events registered successfully");
        } catch (Exception e) {
            VSSkirmish.LOG.error("Failed to register Skirmish events", e);
        }
    }
}
