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
    public static int execute(CommandContext<ServerCommandSource> ctx) {

        ServerPlayerEntity opp = ctx.getSource().getPlayer();
        if (opp == null) {
            return 0;
        }

        // ============================================================
        // PARTY DATA
        // ============================================================
        OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
        IPartyManagerAPI pm = api.getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(opp.getUuid());

        if (party == null) {
            opp.sendMessage(Text.literal("§cYou are not the owner of a party."), false);
            return 0;
        }

        // ============================================================
        // VALIDATE CHALLENGE
        // ============================================================
        SkirmishChallenge challenge = SkirmishManager.INSTANCE.getCurrentChallenge();

        if (challenge == null) {
            opp.sendMessage(Text.literal("§cThere are no challenges to accept"), false);
            return 0;
        }

        if (!challenge.getOppPartyId().equals(party.getId())) {
            opp.sendMessage(Text.literal("§cThere are no challenges to accept"), false);
            return 0;
        }

        // ============================================================
        // CHECK WAGER
        // ============================================================

        ServerPlayerEntity ch = ctx.getSource().getServer().getPlayerManager().getPlayer(challenge.getChLeaderId());
        if (ch == null) {
            opp.sendMessage(Text.literal("Could not find challenger"));
            return 0;
        }

        long wager = challenge.getWager();

        CurrencyComponent oppWallet = ModComponents.CURRENCY.get(opp);
        long oppMoney = oppWallet.getValue();
        if (oppMoney < wager) {
            opp.sendMessage(Text.literal("You don't have enough gold in your wallet for that wager!"));
            return 0;
        }

        CurrencyComponent chWallet = ModComponents.CURRENCY.get(ch);
        long chMoney = chWallet.getValue();
        if (chMoney < wager) {
            opp.sendMessage(Text.literal("Challenger doesn't have enough gold in their wallet for that wager!"));
            return 0;
        }

        // ============================================================
        // START SKIRMISH
        // ============================================================

        SkirmishManager.INSTANCE.startSkirmish(ctx.getSource().getServer(), ch, opp);
        return 1;
    }

}
