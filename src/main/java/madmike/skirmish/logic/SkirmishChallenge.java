package madmike.skirmish.logic;

import madmike.skirmish.config.SkirmishConfig;
import net.minecraft.structure.StructureTemplate;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.UUID;

public class SkirmishChallenge {
    UUID chPartyId;
    StructureTemplate chShip;

    UUID oppPartyId;

    long wager;
    long expiresAt;

    public SkirmishChallenge(UUID chPartyId, StructureTemplate chShip, UUID oppPartyId, long wager) {
        this.chPartyId = chPartyId;
        this.chShip = chShip;
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
