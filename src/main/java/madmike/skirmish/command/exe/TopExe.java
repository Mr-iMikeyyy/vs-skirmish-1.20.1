package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.component.components.StatsComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TopExe {
    public static int executeTop(CommandContext<ServerCommandSource> ctx) {

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("You must be a player to use this command"));
            return 0;
        }

        // ============================================================
        // PARTY DATA
        // ============================================================
        MinecraftServer server = ctx.getSource().getServer();

        StatsComponent sc = SkirmishComponents.STATS.get(server.getScoreboard());
        player.sendMessage(sc.getPrintableTopStats(server));
        return 1;
    }
}
