package madmike.skirmish.event;

import madmike.skirmish.event.events.AllowDeathEvent;
import madmike.skirmish.event.events.EndServerTickEvent;
import madmike.skirmish.event.events.JoinEvent;

public class SkirmishEvents {
    public static void register() {

        AllowDeathEvent.register();

        EndServerTickEvent.register();

        JoinEvent.register();

    }
}
