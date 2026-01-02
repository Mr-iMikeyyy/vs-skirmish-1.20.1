package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.cc.component.CCComponents;
import madmike.cc.logic.BusyPlayers;
import madmike.skirmish.logic.Skirmish;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public class SpectatePlayerExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("You must be a player to use this command"));
            return 0;
        }
        PlayerManager pm = ctx.getSource().getServer().getPlayerManager();
        String name = ctx.getArgument("player", String.class);
        ServerPlayerEntity target = pm.getPlayer(name);
        if (target == null) {
            player.sendMessage(Text.literal("Error finding target player"));
            return 0;
        }
        Skirmish skirmish = SkirmishManager.INSTANCE.getCurrentSkirmish();
        if (skirmish == null) {
            player.sendMessage(Text.literal("Skirmish doesn't exist"));
            return 0;
        }
        if (!skirmish.isPlayerInSkirmish(target.getUuid())) {
            player.sendMessage(Text.literal("That player is not in a skirmish"));
            return 0;
        }
        if (skirmish.isPlayerInSkirmish(player.getUuid())) {
            player.sendMessage(Text.literal("You cannot spectate a skirmish you are in"));
            return 0;
        }
        CCComponents.TP.get(ctx.getSource().getServer().getScoreboard()).set(player.getUuid(), player.getBlockPos(), player.getServerWorld().getRegistryKey());
        BlockPos targetPos = target.getBlockPos();
        player.teleport(target.getServerWorld(), targetPos.getX(), targetPos.getY(), targetPos.getZ(), player.getYaw(), player.getPitch());
        player.changeGameMode(GameMode.SPECTATOR);
        skirmish.addSpectator(player.getUuid());
        BusyPlayers.add(player.getUuid());
        player.sendMessage(Text.literal("You are now spectating and have been teleported to " + name));
        return 1;
    }
}
