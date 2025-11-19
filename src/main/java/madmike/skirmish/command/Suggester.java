package madmike.skirmish.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Suggester {
    public static CompletableFuture<Suggestions> sugChallengeTeam(CommandContext<ServerCommandSource> src, SuggestionsBuilder builder) {
        Set<UUID> ownerIds = new HashSet<>();
        MinecraftServer server = src.getSource().getServer();
        OpenPACServerAPI api = OpenPACServerAPI.get(server);

        api.getPartyManager().getAllStream().forEach(t -> ownerIds.add(t.getOwner().getUUID()));
        for (UUID id : ownerIds) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player != null) {
                builder.suggest(api.getPlayerConfigs().getLoadedConfig(id).getEffective(PlayerConfigOptions.PARTY_NAME));
            }
        }
        return builder.buildFuture();
    }
}
