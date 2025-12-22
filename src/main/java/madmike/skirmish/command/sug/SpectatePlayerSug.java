package madmike.skirmish.command.sug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import madmike.skirmish.logic.Skirmish;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SpectatePlayerSug {
    public static CompletableFuture<Suggestions> suggest(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {

        Skirmish skirmish = SkirmishManager.INSTANCE.getCurrentSkirmish();
        if (skirmish == null) {
            return builder.buildFuture();
        }

        Set<UUID> players = skirmish.getStillAlive();
        PlayerManager pm = ctx.getSource().getServer().getPlayerManager();
        for (UUID id : players) {
            ServerPlayerEntity player = pm.getPlayer(id);
            if (player != null) {
                builder.suggest(player.getGameProfile().getName());
            }
        }

        return builder.buildFuture();
    }
}
