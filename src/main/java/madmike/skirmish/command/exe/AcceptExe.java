package madmike.skirmish.command.exe;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.logic.SkirmishChallenge;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class AcceptExe {
    public static int executeAccept(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        // Get party data
        OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
        IPartyManagerAPI pm = api.getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());

        if (party == null) {
            player.sendMessage(Text.literal("§cYou are not the owner of a party."), false);
            return 0;
        }

        SkirmishChallenge challenge = SkirmishManager.INSTANCE.getCurrentChallenge();

        if (challenge == null || !challenge.getOppPartyId().equals(party.getId())) {
            player.sendMessage(Text.literal("§cThere are no challenges to accept"), false);
            return 0;
        }

        long wager = challenge.getWager();
        CurrencyComponent cc = ModComponents.CURRENCY.get(player);
        long wallet = cc.getValue();

        if (wallet < wager) {
            player.sendMessage(Text.literal("You don't have enough gold in your wallet for that wager!"));
            return 0;
        }

        cc.modify(-wager);

        SkirmishManager.INSTANCE.startSkirmish(ctx.getSource().getServer(), challenge);
        return 1;
    }
}
