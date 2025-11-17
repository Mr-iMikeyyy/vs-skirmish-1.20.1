package madmike.skirmish.logic;

import madmike.skirmish.config.SkirmishConfig;

import java.util.UUID;

public class SkirmishChallenge {
    UUID chPartyId;
    UUID chShipId;

    UUID oppPartyId;
    UUID oppShipId;

    int wager;
    long expiresAt;

    public SkirmishChallenge(UUID chPartyId, UUID chShipId, UUID oppPartyId, UUID oppShipId, int wager) {
        this.chPartyId = chPartyId;
        this.chShipId = chShipId;
        this.oppPartyId = oppPartyId;
        this.oppShipId = oppShipId;
        this.wager = wager;
        this.expiresAt = (SkirmishConfig.skirmishChallengeMaxTime * 10000L) + System.currentTimeMillis();
    }
}
