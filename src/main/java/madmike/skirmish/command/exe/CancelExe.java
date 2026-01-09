package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.logic.SkirmishChallenge;
import madmike.skirmish.logic.SkirmishChallengeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class CancelExe {
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
        IServerPartyAPI party = OpenPACServerAPI.get(server).getPartyManager().getPartyByOwner(player.getUuid());

        if (party == null) {
            player.sendMessage(Text.literal("§cYou are not the owner of a party."), false);
            return 0;
        }

        // ============================================================
        // VALIDATE CHALLENGE
        // ============================================================
        SkirmishChallenge challenge = SkirmishChallengeManager.INSTANCE.getOutgoingChallengeFor(party.getId());

        if (challenge == null) {
            player.sendMessage(Text.literal("§cThere are no challenges to cancel"), false);
            return 0;
        }

        // ============================================================
        // DENY CHALLENGE
        // ============================================================
        SkirmishChallengeManager.INSTANCE.removeChallenge(server, challenge, "The skirmish challenge was cancelled");
        return 1;
    }
}
