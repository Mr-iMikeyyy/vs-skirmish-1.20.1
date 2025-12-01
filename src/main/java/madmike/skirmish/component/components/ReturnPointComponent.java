package madmike.skirmish.component.components;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReturnPointComponent implements ComponentV3 {

    private final Map<UUID, StoredReturn> returnPoints = new HashMap<>();

    public void tpBack(MinecraftServer server, ServerPlayerEntity player) {
        StoredReturn point = returnPoints.get(player.getUuid());
        if (point != null) {
            player.teleport(server.getWorld(point.dim), point.pos.getX(), point.pos.getY(), point.pos.getZ(), player.getYaw(), player.getPitch());
        }
    }

    public record StoredReturn(BlockPos pos, RegistryKey<World> dim) { }

    private final Scoreboard provider;
    private final MinecraftServer server;

    public ReturnPointComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    // === Store ===

    public void set(UUID playerId, BlockPos pos, RegistryKey<World> dim) {
        if (playerId != null && pos != null && dim != null) {
            returnPoints.put(playerId, new StoredReturn(pos, dim));
        }
    }

    public void tpPlayerBack(ServerPlayerEntity player) {
        UUID id = player.getUuid();

        StoredReturn data = returnPoints.remove(id);

        if (data != null) {
            ServerWorld world = server.getWorld(data.dim());
            if (world != null) {
                player.teleport(world,
                        data.pos().getX() + 0.5,
                        data.pos().getY(),
                        data.pos().getZ() + 0.5,
                        player.getYaw(), player.getPitch());
                player.sendMessage(Text.literal("§7You’ve been returned to your previous location."));
            }
        }
    }

    // === Serialization ===

    @Override
    public void readFromNbt(NbtCompound nbt) {
        returnPoints.clear();
        NbtCompound all = nbt.getCompound("return_points");

        for (String key : all.getKeys()) {
            UUID id = UUID.fromString(key);
            NbtCompound tag = all.getCompound(key);

            BlockPos pos = new BlockPos(
                    tag.getInt("x"),
                    tag.getInt("y"),
                    tag.getInt("z")
            );
            String dimStr = tag.getString("dim");
            RegistryKey<World> dim = RegistryKey.of(RegistryKeys.WORLD, new Identifier(dimStr));

            returnPoints.put(id, new StoredReturn(pos, dim));
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        NbtCompound all = new NbtCompound();

        for (Map.Entry<UUID, StoredReturn> e : returnPoints.entrySet()) {
            StoredReturn data = e.getValue();
            NbtCompound tag = new NbtCompound();

            tag.putInt("x", data.pos().getX());
            tag.putInt("y", data.pos().getY());
            tag.putInt("z", data.pos().getZ());
            tag.putString("dim", data.dim().getValue().toString());

            all.put(e.getKey().toString(), tag);
        }

        nbt.put("return_points", all);
    }
}
