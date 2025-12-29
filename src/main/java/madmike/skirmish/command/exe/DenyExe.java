package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.cc.logic.BusyPlayers;
import madmike.skirmish.logic.SkirmishChallenge;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class DenyExe {
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
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());

        if (party == null) {
            player.sendMessage(Text.literal("§cYou are not the owner of a party."), false);
            return 0;
        }

        // ============================================================
        // VALIDATE CHALLENGE
        // ============================================================
        SkirmishChallenge challenge = SkirmishManager.INSTANCE.getCurrentChallenge();

        if (challenge == null) {
            player.sendMessage(Text.literal("§cThere are no challenges to deny"), false);
            return 0;
        }

        if (!challenge.getOppPartyId().equals(party.getId())) {
            player.sendMessage(Text.literal("§cYour party is not the target of the current challenge"), false);
            return 0;
        }

        // ============================================================
        // DENY CHALLENGE
        // ============================================================

        challenge.broadcastMsg(server, "The skirmish challenge was denied");

        party.getOnlineMemberStream().forEach(p -> {
            BusyPlayers.remove(p.getUuid());
        });

        IServerPartyAPI chParty = pm.getPartyById(challenge.getChPartyId());
        if (chParty != null) {
            chParty.getOnlineMemberStream().forEach(p -> {
                BusyPlayers.remove(p.getUuid());
            });
        }

        challenge.end(server, Text.literal("Opponents denied the challenge"));

        return 1;
    }
}
