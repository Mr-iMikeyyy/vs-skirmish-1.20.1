package madmike.skirmish.component.components;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NamesComponent implements ComponentV3 {
    private final Map<UUID, String> names = new HashMap<>();
    private final Scoreboard provider;
    private final MinecraftServer server;

    public NamesComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    public String getName(UUID playerId) {
        return names.get(playerId);
    }

    public void onPlayerLogin(ServerPlayerEntity player) {
        names.put(player.getUuid(), player.getGameProfile().getName());
    }

    @Override
    public void readFromNbt(NbtCompound nbt) {
        names.clear();
        NbtList list = nbt.getList("Names", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            names.put(tag.getUuid("Id"), tag.getString("Name"));
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        names.forEach((id, name) -> {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Id", id);
            tag.putString("Name", name);
            list.add(tag);
        });
        nbt.put("Names", list);
    }
}
