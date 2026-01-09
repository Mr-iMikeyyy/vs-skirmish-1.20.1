package madmike.skirmish.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public class SkirmishManager {

    private Skirmish currentSkirmish;

    private static final int CHECK_INTERVAL_TICKS = 200; // 10 seconds
    private static int tickCounter = 0;

    public static final SkirmishManager INSTANCE = new SkirmishManager();

    private SkirmishManager() {}

    public Skirmish getCurrentSkirmish() {
        return currentSkirmish;
    }

    /* ================= tick ================= */
    public void tick(MinecraftServer server) {
        if (currentSkirmish != null) {
            if (currentSkirmish.isExpired()) {
                currentSkirmish.end(server, EndOfSkirmishType.TIME);
            }
            if (currentSkirmish.hasCountdown()) {
                currentSkirmish.countDownTick(server);
            }
//            tickCounter++;
//
//            if (tickCounter >= CHECK_INTERVAL_TICKS) {
//                tickCounter = 0;
//                currentSkirmish.runDistanceCheck(server);
//            }
        }
        else {
            tryToStartSkirmish(server);
        }
    }

    /* ================= start ================= */
    public void tryToStartSkirmish(MinecraftServer server) {
        SkirmishChallenge challenge = SkirmishChallengeManager.INSTANCE.getOldestAcceptedChallenge();

        if (challenge == null) {
            return;
        }

        Skirmish skirmish = new Skirmish();

        boolean started = skirmish.start(server, challenge);

        if (started) {
            currentSkirmish = skirmish;
        }
    }

    /* ================= reset ================= */
    public void resetMatch() {
        currentSkirmish = null;
    }

    /* ================= event ================= */
    public boolean handlePlayerDeath(ServerPlayerEntity player) {
        if (currentSkirmish != null) {
            return currentSkirmish.handlePlayerDeath(player);
        }
        return true;
    }
    public void handlePlayerQuit(MinecraftServer server, ServerPlayerEntity player) {
        if (currentSkirmish != null) {
            currentSkirmish.handlePlayerQuit(server, player);
        }
    }
    public boolean isMovementLocked(UUID playerId) {
        if (currentSkirmish == null) {
            return false;
        }
        if (!currentSkirmish.hasCountdown()) {
            return false;
        }
        return currentSkirmish.isPlayerInSkirmish(playerId);
    }

    /* ================= ships ================= */
    public boolean isShipInSkirmish(long id) {
        if (currentSkirmish == null) {
            return false;
        }
        return currentSkirmish.isShipInSkirmish(id);
    }
    public void endSkirmishForShip(MinecraftServer server, long id) {
        if (currentSkirmish.isChallengerShip(id)) {
            currentSkirmish.end(server, EndOfSkirmishType.OPPONENTS_WIN_SHIP);
        }
        else {
            currentSkirmish.end(server, EndOfSkirmishType.CHALLENGERS_WIN_SHIP);
        }
    }




}
