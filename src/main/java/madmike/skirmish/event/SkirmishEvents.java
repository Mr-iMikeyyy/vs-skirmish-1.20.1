package madmike.skirmish.event;

import madmike.skirmish.event.events.*;

public class SkirmishEvents {
    public static void register() {

        AllowDeathEvent.register();

        DisconnectEvent.register();

        EndServerTickEvent.register();

        EntitySpawnEvent.register();

        JoinEvent.register();

        ShipHelmBlockDestroyedEvent.register();
    }
}
