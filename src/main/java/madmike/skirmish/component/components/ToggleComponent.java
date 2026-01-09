package madmike.skirmish.component.components;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ToggleComponent implements ComponentV3 {
    private static final String KEY_ENABLED = "enabledParties";

    private final Set<UUID> enabledParties = new HashSet<>();

    private final Scoreboard provider;
    private final MinecraftServer server;

    public ToggleComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    // ============================================================
    // Toggle API
    // ============================================================

    public void toggleOn(UUID partyId) {
        enabledParties.add(partyId);
    }

    public void toggleOff(UUID partyId) {
        enabledParties.remove(partyId);
    }

    public boolean isReady(UUID partyId) {
        return enabledParties.contains(partyId);
    }

    public Set<UUID> getEnabledParties() {
        return enabledParties;
    }

    // ============================================================
    // NBT: Save
    // ============================================================

    @Override
    public void writeToNbt(NbtCompound nbt) {
        NbtList list = new NbtList();

        for (UUID id : enabledParties) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("id", id);
            list.add(entry);
        }

        nbt.put(KEY_ENABLED, list);
    }

    // ============================================================
    // NBT: Load
    // ============================================================

    @Override
    public void readFromNbt(NbtCompound nbt) {
        enabledParties.clear();

        if (nbt.contains(KEY_ENABLED, NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList(KEY_ENABLED, NbtElement.COMPOUND_TYPE);

            for (NbtElement el : list) {
                if (el instanceof NbtCompound entry && entry.containsUuid("id")) {
                    enabledParties.add(entry.getUuid("id"));
                }
            }
        }
    }


}
