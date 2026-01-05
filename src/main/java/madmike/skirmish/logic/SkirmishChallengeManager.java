package madmike.skirmish.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class SkirmishChallengeManager {
    public static final SkirmishChallengeManager INSTANCE = new SkirmishChallengeManager();

    private final Set<SkirmishChallenge> challenges = new HashSet<>();

    public void tick(MinecraftServer server) {
        if (!challenges.isEmpty()) {
            Iterator<SkirmishChallenge> it = challenges.iterator();
            while (it.hasNext()) {
                SkirmishChallenge challenge = it.next();

                if (challenge.isExpired() && !challenge.isAccepted()) {
                    challenge.end(server, "TDM Challenge has expired");
                    it.remove();
                }
            }
        }
    }

    public void addChallenge(SkirmishChallenge challenge) {
        challenges.add(challenge);
    }

    public void removeChallenge(MinecraftServer server, SkirmishChallenge challenge, String msg) {
        challenge.end(server, msg);
        challenges.remove(challenge);
    }

    public SkirmishChallenge getOldestAcceptedChallenge() {
        return challenges.stream()
                .filter(SkirmishChallenge::isAccepted)
                .min(Comparator.comparingLong(SkirmishChallenge::getExpiresAt))
                .orElse(null);
    }

    public void handlePlayerQuit(MinecraftServer server, ServerPlayerEntity player) {
        if (!challenges.isEmpty()) {
            Iterator<SkirmishChallenge> it = challenges.iterator();
            while (it.hasNext()) {
                SkirmishChallenge challenge = it.next();

                if (challenge.getChLeader(server).equals(player) || challenge.getOppLeader(server).equals(player)) {
                    challenge.end(server, "One of the party leaders has quit, cancelling match");
                    it.remove();
                }
            }
        }
    }

    public SkirmishChallenge getIncomingChallengeFor(UUID partyId) {
        if (!challenges.isEmpty()) {
            for (SkirmishChallenge challenge : challenges) {
                if (challenge.getOppPartyId().equals(partyId)) {
                    return challenge;
                }
            }
        }
        return null;
    }

    public SkirmishChallenge getOutgoingChallengeFor(UUID partyId) {
        if (!challenges.isEmpty()) {
            for (SkirmishChallenge challenge : challenges) {
                if (challenge.getChPartyId().equals(partyId)) {
                    return challenge;
                }
            }
        }
        return null;
    }
}
