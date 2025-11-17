package madmike.skirmish.component.components;

import com.glisco.numismaticoverhaul.ModComponents;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import madmike.skirmish.logic.SkirmishChallenge;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RefundComponent implements ComponentV3 {

    // Refunds stored per player UUID
    private final Map<UUID, Long> storedRefunds = new HashMap<>();

    private final Scoreboard provider;
    private final MinecraftServer server;

    public RefundComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    /**
     * Refunds wager amounts to both players.
     * If a player is offline, the refund is stored until next login.
     */
    public void refundChallenge(MinecraftServer server, SkirmishChallenge challenge) {
        PlayerManager pm = server.getPlayerManager();

        refundPlayer(pm, challenge.challengerId, challenge.wager);
        refundPlayer(pm, challenge.opponentId, challenge.wager);
    }

    public void refundPlayer(PlayerManager pm, UUID playerId, int wager) {
        ServerPlayerEntity player = pm.getPlayer(playerId);
        long amount = wager * 10000L;

        if (player != null) {
            ModComponents.CURRENCY.get(player).modify(amount);
        } else {
            storedRefunds.merge(playerId, amount, Long::sum); // accumulate if multiple refunds stack
        }
    }

    /**
     * Called when a player logs in.
     * If they have stored refunds, give them the coins and clear the record.
     */
    public void onPlayerLogin(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Long refund = storedRefunds.remove(id);
        if (refund != null && refund > 0) {
            ModComponents.CURRENCY.get(player).modify(refund);
            player.sendMessage(Text.literal("You’ve received a refund"));
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        storedRefunds.clear();
        NbtList list = tag.getList("refunds", NbtElement.COMPOUND_TYPE);

        for (NbtElement el : list) {
            NbtCompound refundTag = (NbtCompound) el;
            UUID id = UUID.fromString(refundTag.getString("id"));
            long amount = refundTag.getLong("amount");
            storedRefunds.put(id, amount);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Long> entry : storedRefunds.entrySet()) {
            NbtCompound sub = new NbtCompound();
            sub.putString("id", entry.getKey().toString());
            sub.putLong("amount", entry.getValue());
            list.add(sub);
        }
        tag.put("refunds", list);
    }


}
