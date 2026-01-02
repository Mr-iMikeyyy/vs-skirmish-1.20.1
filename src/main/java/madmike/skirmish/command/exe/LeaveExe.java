package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.logic.Skirmish;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class LeaveExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("You must be a player to use this command"));
            return 0;
        }
        Skirmish skirmish = SkirmishManager.INSTANCE.getCurrentSkirmish();
        if (skirmish == null) {
            player.sendMessage(Text.literal("No Skirmish Found"));
            return 0;
        }
        UUID id = player.getUuid();
        if (!skirmish.isPlayerSpectating(id)) {
            player.sendMessage(Text.literal("You are not currently spectating"));
            return 0;
        }
        skirmish.removeSpectator(ctx.getSource().getServer(), player);
        return 1;
    }
}
