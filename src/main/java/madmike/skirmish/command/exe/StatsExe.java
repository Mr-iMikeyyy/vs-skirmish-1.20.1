package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.component.components.StatsComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class StatsExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("You must be a player to use this command"));
            return 0;
        }

        // ============================================================
        // PARTY DATA
        // ============================================================
        MinecraftServer server = ctx.getSource().getServer();
        OpenPACServerAPI api = OpenPACServerAPI.get(server);
        IPartyManagerAPI pm = api.getPartyManager();
        IServerPartyAPI party = pm.getPartyByMember(player.getUuid());

        if (party == null) {
            player.sendMessage(Text.literal("§cYou are not in a party."), false);
            return 0;
        }

        StatsComponent sc = SkirmishComponents.STATS.get(server.getScoreboard());
        player.sendMessage(sc.getPrintableStats(party.getId()));
        return 1;
    }
}
