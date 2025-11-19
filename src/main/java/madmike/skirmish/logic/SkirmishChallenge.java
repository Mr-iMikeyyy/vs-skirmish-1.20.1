package madmike.skirmish.logic;

import madmike.skirmish.config.SkirmishConfig;

import java.util.UUID;

public class SkirmishChallenge {
    UUID chPartyId;

    UUID oppPartyId;

    long wager;
    long expiresAt;

    public SkirmishChallenge(UUID chPartyId, UUID oppPartyId, long wager) {
        this.chPartyId = chPartyId;
        this.oppPartyId = oppPartyId;
        this.wager = wager;
        this.expiresAt = (SkirmishConfig.skirmishChallengeMaxTime * 1000L) + System.currentTimeMillis();
    }

    public UUID getChPartyId() {
        return chPartyId;
    }

    public UUID getOppPartyId() {
        return oppPartyId;
    }

    public long getWager() {
        return wager;
    }

    public long getExpiresAt() {
        return expiresAt;
    }


}
