package madmike.skirmish.logic;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.Set;
import java.util.UUID;

public class Skirmish {
    private SkirmishChallenge challenge;

    private Set<UUID> challengers;

    private Set<UUID> opponents;

    private Set<UUID> spectators;

    private long expiresAt;

    public SkirmishChallenge getChallenge() {
        return challenge;
    }

    public long getExpiredTime() {
        return expiresAt;
    }

    public boolean isPlayerInSkirmish(ServerPlayerEntity player) {
        return challengers.contains(player.getUuid()) || opponents.contains(player.getUuid());
    }

    public void handlePlayerDeath(ServerPlayerEntity player) {

        UUID id = player.getUuid();

        opponents.remove(id);
        challengers.remove(id);

        if (opponents.isEmpty()) {
            SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN);
        }

        player.setHealth(player.getMaxHealth());
        player.changeGameMode(GameMode.SPECTATOR);
        player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));

    }
}
