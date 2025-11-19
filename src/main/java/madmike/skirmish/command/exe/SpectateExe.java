package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.logic.Skirmish;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SpectateExe {
    public static int executeSpectate(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Must be a player to use this command"));
            return 0;
        }

        Skirmish skirmish = SkirmishManager.INSTANCE.getCurrentSkirmish();
        if (skirmish == null) {
            player.sendMessage(Text.literal("There is no skirmish to watch right now"));
            return 0;
        }

        if (skirmish.isPlayerInSkirmish(player)) {
            player.sendMessage(Text.literal("You cannot spectate a skirmish you are participating in"));
            return 0;
        }

        SkirmishManager.INSTANCE.addSpectator(player);
        return 1;
    }
}
