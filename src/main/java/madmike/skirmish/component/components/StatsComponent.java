package madmike.skirmish.component.components;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatsComponent implements ComponentV3 {

    private final Map<UUID, SkirmishStat> stats = new HashMap<>();

    private final Scoreboard provider;
    private final MinecraftServer server;

    public StatsComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    // Ensure the stat exists for a player
    private SkirmishStat getOrCreate(UUID partyId) {
        return stats.computeIfAbsent(partyId, id -> new SkirmishStat(
                0, 0, 0, 0, 0, 0
        ));
    }

    // General purpose update method
    public void setPartySkirmishStats(
            UUID partyId,
            int deltaWins,
            int deltaLosses,
            int wagerGained,
            int wagerLost,
            int deltaShipsLost,
            int deltaShipsDestroyed
    ) {
        SkirmishStat old = getOrCreate(partyId);

        stats.put(partyId, new SkirmishStat(
                old.wins() + deltaWins,
                old.losses() + deltaLosses,
                old.wagesWon() + wagerGained,
                old.wagesLost() + wagerLost,
                old.shipsLost() + deltaShipsLost,
                old.shipsDestroyed() + deltaShipsDestroyed
        ));
    }

    // Convenience wrappers
    public void incrementWin(UUID partyId, int wagerGained) {
        setPartySkirmishStats(partyId, 1, 0, wagerGained, 0, 0, 0);
    }

    public void incrementLoss(UUID partyId, int wagerLost) {
        setPartySkirmishStats(partyId, 0, 1, 0, wagerLost, 0, 0);
    }

    public void incrementShipLost(UUID partyId) {
        setPartySkirmishStats(partyId, 0, 0, 0, 0, 1, 0);
    }

    public void incrementShipDestroyed(UUID partyId) {
        setPartySkirmishStats(partyId, 0, 0, 0, 0, 0, 1);
    }

    // Printable stats
    public Text getPrintableStats(UUID partyId) {
        SkirmishStat stat = stats.get(partyId);

        if (stat == null) {
            return Text.literal("No skirmish stats found.").formatted(Formatting.GRAY);
        }

        int wins = stat.wins();
        int losses = stat.losses();
        int wagersWon = stat.wagesWon();
        int wagersLost = stat.wagesLost();
        int shipsLost = stat.shipsLost();
        int shipsDestroyed = stat.shipsDestroyed();

        double kdr = losses == 0 ? wins : (double) wins / losses;
        int totalEarnings = wagersWon - wagersLost;

        MutableText text = Text.literal("📜 Skirmish Stats:\n").formatted(Formatting.GOLD);

        text.append(Text.literal("Wins: ").formatted(Formatting.GREEN))
                .append(Text.literal(String.valueOf(wins)).formatted(Formatting.WHITE)).append("\n");

        text.append(Text.literal("Losses: ").formatted(Formatting.RED))
                .append(Text.literal(String.valueOf(losses)).formatted(Formatting.WHITE)).append("\n");

        text.append(Text.literal("K/D Ratio: ").formatted(Formatting.YELLOW))
                .append(Text.literal(String.format("%.2f", kdr)).formatted(Formatting.WHITE)).append("\n");

        text.append(Text.literal("Ships Destroyed: ").formatted(Formatting.BLUE))
                .append(Text.literal(String.valueOf(shipsDestroyed)).formatted(Formatting.WHITE)).append("\n");

        text.append(Text.literal("Ships Lost: ").formatted(Formatting.DARK_RED))
                .append(Text.literal(String.valueOf(shipsLost)).formatted(Formatting.WHITE)).append("\n");

        text.append(Text.literal("Wager Won: ").formatted(Formatting.GREEN))
                .append(Text.literal(wagersWon + "g").formatted(Formatting.WHITE)).append("\n");

        text.append(Text.literal("Wager Lost: ").formatted(Formatting.RED))
                .append(Text.literal(wagersLost + "g").formatted(Formatting.WHITE)).append("\n");

        text.append(Text.literal("Net Earnings: ")
                        .formatted(totalEarnings >= 0 ? Formatting.GREEN : Formatting.RED))
                .append(Text.literal(totalEarnings + "g").formatted(Formatting.WHITE));

        return text;
    }

    // Top stats (sorted by earnings)
    public Text getPrintableTopStats(MinecraftServer server) {
        if (stats.isEmpty()) {
            return Text.literal("No skirmish stats recorded yet.").formatted(Formatting.GRAY);
        }

        List<Map.Entry<UUID, SkirmishStat>> sorted = stats.entrySet().stream()
                .sorted((a, b) -> {
                    int aEarnings = a.getValue().wagesWon() - a.getValue().wagesLost();
                    int bEarnings = b.getValue().wagesWon() - b.getValue().wagesLost();
                    return Integer.compare(bEarnings, aEarnings);
                })
                .limit(10)
                .toList();

        MutableText topText = Text.literal("🏆 Top Parties:\n").formatted(Formatting.GOLD);

        IPartyManagerAPI pm = OpenPACServerAPI.get(server).getPartyManager();
        IPlayerConfigManagerAPI pc = OpenPACServerAPI.get(server).getPlayerConfigs();

        int rank = 1;
        for (Map.Entry<UUID, SkirmishStat> entry : sorted) {
            UUID partyId = entry.getKey();
            IPartyMemberAPI owner = pm.getPartyById(partyId).getOwner();
            String partyName = pc.getLoadedConfig(owner.getUUID())
                    .getFromEffectiveConfig(PlayerConfigOptions.PARTY_NAME);

            SkirmishStat stat = entry.getValue();
            int earnings = stat.wagesWon() - stat.wagesLost();

            topText.append(Text.literal(rank + ". " + partyName + " ").formatted(Formatting.YELLOW))
                    .append(Text.literal("[" + stat.wins() + "W/" + stat.losses() + "L] ").formatted(Formatting.GRAY))
                    .append(Text.literal("S-" + stat.shipsDestroyed()).formatted(Formatting.BLUE))
                    .append(Text.literal("/" + stat.shipsLost()).formatted(Formatting.DARK_RED))
                    .append(Text.literal(" " + earnings + "g")
                            .formatted(earnings >= 0 ? Formatting.GREEN : Formatting.RED))
                    .append(Text.literal("\n"));

            rank++;
        }

        return topText;
    }

    // ---- NBT Serialization ----
    @Override
    public void readFromNbt(NbtCompound tag) {
        stats.clear();
        NbtList list = tag.getList("skirmish_stats", NbtElement.COMPOUND_TYPE);

        for (NbtElement el : list) {
            NbtCompound entry = (NbtCompound) el;
            UUID id = UUID.fromString(entry.getString("id"));
            int wins = entry.getInt("wins");
            int losses = entry.getInt("losses");
            int wagesWon = entry.getInt("wagesWon");
            int wagesLost = entry.getInt("wagesLost");
            int shipsLost = entry.getInt("shipsLost");
            int shipsDestroyed = entry.getInt("shipsDestroyed");

            stats.put(id, new SkirmishStat(wins, losses, wagesWon, wagesLost, shipsLost, shipsDestroyed));
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();

        for (Map.Entry<UUID, SkirmishStat> entry : stats.entrySet()) {
            NbtCompound sub = new NbtCompound();
            SkirmishStat s = entry.getValue();

            sub.putString("id", entry.getKey().toString());
            sub.putInt("wins", s.wins());
            sub.putInt("losses", s.losses());
            sub.putInt("wagesWon", s.wagesWon());
            sub.putInt("wagesLost", s.wagesLost());
            sub.putInt("shipsLost", s.shipsLost());
            sub.putInt("shipsDestroyed", s.shipsDestroyed());

            list.add(sub);
        }

        tag.put("skirmish_stats", list);
    }

    // Updated record
    public record SkirmishStat(
            int wins,
            int losses,
            int wagesWon,
            int wagesLost,
            int shipsLost,
            int shipsDestroyed
    ) {}
}
