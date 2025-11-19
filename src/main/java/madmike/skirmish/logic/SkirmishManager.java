package madmike.skirmish.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class SkirmishManager {

    private SkirmishChallenge currentChallenge;

    private Skirmish currentSkirmish;

    public static final SkirmishManager INSTANCE = new SkirmishManager();

    /* ================= Constructor ================= */

    private SkirmishManager() {}

    public SkirmishChallenge getCurrentChallenge() { return currentChallenge; }

    public Skirmish getCurrentSkirmish() {
        return currentSkirmish;
    }

    public void tick(MinecraftServer server) {
        if (currentSkirmish != null) {
            if (currentSkirmish.getExpiredTime() <= System.currentTimeMillis()) {
                endSkirmish(server, EndOfSkirmishType.TIME);
            }
        }
    }

    public void startSkirmish(SkirmishChallenge challenge) {

    }

    public void endSkirmish(MinecraftServer server, EndOfSkirmishType type) {
        // award winners
        // record stats
        // tp back
        // wipe dimension
    }

    public boolean hasChallengeOrSkirmish() {
        return currentSkirmish != null || currentChallenge != null;
    }

    public boolean handlePlayerDeath(ServerPlayerEntity player) {

        if (currentSkirmish == null) {
            return true;
        }

        if (currentSkirmish.isPlayerInSkirmish(player)) {
            currentSkirmish.handlePlayerDeath(player);
            return false;
        }

        return true;
    }


    public void setCurrentChallenge(SkirmishChallenge challenge) {
        this.currentChallenge = challenge;
    }
}
