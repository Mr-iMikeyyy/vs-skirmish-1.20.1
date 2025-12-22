package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.component.components.StatsComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class AdminResetAllStatsExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        StatsComponent sc = SkirmishComponents.STATS.get(server.getScoreboard());
        sc.resetAllStats();
        ctx.getSource().sendMessage(Text.literal("All skirmish stats have been reset!"));
        return 1;
    }
}
