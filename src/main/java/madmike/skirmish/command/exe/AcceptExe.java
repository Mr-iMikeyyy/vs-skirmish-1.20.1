package madmike.skirmish.command.exe;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.VSSkirmish;
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

        VSSkirmish.LOGGER.info("[SKIRMISH] ===== /skirmish accept executed =====");

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: /skirmish accept invoked without a player.");
            return 0;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Player executing accept: {}", player.getGameProfile().getName());

        // ============================================================
        // PARTY DATA
        // ============================================================
        OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
        IPartyManagerAPI pm = api.getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());

        if (party == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Player {} attempted to accept but is not a party owner.",
                    player.getGameProfile().getName());
            player.sendMessage(Text.literal("§cYou are not the owner of a party."), false);
            return 0;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Player's party ID: {}", party.getId());

        // ============================================================
        // VALIDATE CHALLENGE
        // ============================================================
        SkirmishChallenge challenge = SkirmishManager.INSTANCE.getCurrentChallenge();

        if (challenge == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] No pending challenge exists to accept.");
            player.sendMessage(Text.literal("§cThere are no challenges to accept"), false);
            return 0;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Found active challenge. Opponent party ID should be: {}",
                challenge.getOppPartyId());

        if (!challenge.getOppPartyId().equals(party.getId())) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Challenge exists, but it is not targeted at player's party {}",
                    party.getId());
            player.sendMessage(Text.literal("§cThere are no challenges to accept"), false);
            return 0;
        }

        // ============================================================
        // CHECK WAGER
        // ============================================================
        long wager = challenge.getWager();
        CurrencyComponent cc = ModComponents.CURRENCY.get(player);
        long wallet = cc.getValue();

        VSSkirmish.LOGGER.info("[SKIRMISH] Wager required: {} gold ({} raw)", wager, wager * 10000L);
        VSSkirmish.LOGGER.info("[SKIRMISH] Player wallet: {}", wallet);

        if (wallet < wager) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Player {} cannot accept: insufficient funds. Has {}, needs {}.",
                    player.getGameProfile().getName(), wallet, wager);
            player.sendMessage(Text.literal("You don't have enough gold in your wallet for that wager!"));
            return 0;
        }

        // Deduct wager
        cc.modify(-wager);

        VSSkirmish.LOGGER.info("[SKIRMISH] Deducted wager from player {}. New balance: {}",
                player.getGameProfile().getName(), cc.getValue());

        // ============================================================
        // START SKIRMISH
        // ============================================================
        VSSkirmish.LOGGER.info("[SKIRMISH] Starting skirmish between Challenger={} and Opponent={}",
                challenge.getChPartyId(), challenge.getOppPartyId());

        SkirmishManager.INSTANCE.startSkirmish(ctx.getSource().getServer(), challenge);

        VSSkirmish.LOGGER.info("[SKIRMISH] ===== Skirmish start initiated successfully =====");
        return 1;
    }

}
