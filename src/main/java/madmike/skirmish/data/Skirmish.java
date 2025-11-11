package madmike.skirmish.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Skirmish {
    private SkirmishChallenge challenge;
    private Map<UUID, PlayerPreviousLocation> previousLocations = new HashMap<>();
}
