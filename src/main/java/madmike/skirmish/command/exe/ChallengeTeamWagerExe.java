package madmike.skirmish.command.exe;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.logic.SkirmishChallenge;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ChallengeTeamWagerExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {

        VSSkirmish.LOGGER.info("[SKIRMISH] ===== /skirmish challenge <team> <wager> executed =====");

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] ERROR: Command source was not a player.");
            ctx.getSource().sendMessage(Text.literal("You must be a player to use this command"));
            return 0;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Player executing command: {}", player.getGameProfile().getName());

        SkirmishManager sm = SkirmishManager.INSTANCE;
        if (sm.hasChallengeOrSkirmish()) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] A skirmish or challenge is already active. Rejecting request.");
            player.sendMessage(Text.literal("There is already a skirmish or pending challenge, please try again later."));
            return 0;
        }

        MinecraftServer server = ctx.getSource().getServer();
        OpenPACServerAPI api = OpenPACServerAPI.get(server);
        IPartyManagerAPI pm = api.getPartyManager();

        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());
        if (party == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Player {} attempted to challenge but is not a party leader.", player.getName().getString());
            player.sendMessage(Text.literal("You must be a party leader to use this command"));
            return 0;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Challenger Party ID: {}", party.getId());

        if (!SkirmishComponents.TOGGLE.get(server.getScoreboard()).isEnabled(party.getId())) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Challenger party {} has not enabled skirmishes.", party.getId());
            player.sendMessage(Text.literal("Your party has not yet enabled skirmishes"));
            return 0;
        }

        StructureTemplateManager stm = player.getServerWorld().getStructureTemplateManager();
        Identifier selfShipID = new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId());
        Optional<StructureTemplate> ship = stm.getTemplate(selfShipID);

        VSSkirmish.LOGGER.info("[SKIRMISH] Looking for challenger ship template: {}", selfShipID);

        if (ship.isEmpty()) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] No saved ship found for challenger party {}", party.getId());
            player.sendMessage(Text.literal("Your party has no saved ship"));
            return 0;
        }

        String teamName = StringArgumentType.getString(ctx, "team");
        VSSkirmish.LOGGER.info("[SKIRMISH] Target team name provided: {}", teamName);

        IPlayerConfigManagerAPI pc = api.getPlayerConfigs();

        // Collect all party owners
        Set<UUID> ownerIds = new HashSet<>();
        pm.getAllStream().forEach(t -> ownerIds.add(t.getOwner().getUUID()));

        VSSkirmish.LOGGER.info("[SKIRMISH] Scanning {} party owners for matching team name...", ownerIds.size());

        IServerPartyAPI oppParty = null;

        for (UUID id : ownerIds) {
            ServerPlayerEntity otherPlayer = server.getPlayerManager().getPlayer(id);
            if (otherPlayer != null) {
                String partyName = pc.getLoadedConfig(id).getEffective(PlayerConfigOptions.PARTY_NAME);
                if (partyName.isEmpty()) {
                    partyName = party.getDefaultName();
                }

                VSSkirmish.LOGGER.info("[SKIRMISH] Checking party name '{}' for player {}", partyName, otherPlayer.getGameProfile().getName());

                if (partyName.equals(teamName)) {
                    oppParty = api.getPartyManager().getPartyByOwner(id);
                    VSSkirmish.LOGGER.info("[SKIRMISH] Found matching opponent party: {}", oppParty.getId());
                    break;
                }
            }
        }

        if (oppParty == null) {
            VSSkirmish.LOGGER.error("[SKIRMISH] Could not find opponent party with name '{}'", teamName);
            player.sendMessage(Text.literal("Error finding other team."));
            return 0;
        }

        ServerPlayerEntity oppLeader = server.getPlayerManager().getPlayer(oppParty.getOwner().getUUID());
        if (oppLeader == null) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Opponent party {} leader is offline.", oppParty.getId());
            player.sendMessage(Text.literal("Opponent team leader is offline"));
            return 0;
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Opponent Party ID: {}, Leader: {}",
                oppParty.getId(), oppLeader.getGameProfile().getName());

        if (!SkirmishComponents.TOGGLE.get(server.getScoreboard()).isEnabled(oppParty.getId())) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Opponent party {} has not enabled skirmishes.", oppParty.getId());
            player.sendMessage(Text.literal("Opponent party has not yet enabled skirmishes"));
            return 0;
        }

        Identifier oppShipID = new Identifier(VSSkirmish.MOD_ID, "ships/" + oppParty.getId());
        VSSkirmish.LOGGER.info("[SKIRMISH] Looking for opponent ship template: {}", oppShipID);
        Optional<StructureTemplate> oppShip = stm.getTemplate(oppShipID);

        if (oppShip.isEmpty()) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] No saved ship found for opponent party {}", oppParty.getId());
            player.sendMessage(Text.literal("Could not find a ship for the other party"));
            return 0;
        }

        // WAGER CHECK
        int wagerInt = IntegerArgumentType.getInteger(ctx, "wager");
        long wager = wagerInt * 10000L;

        CurrencyComponent cc = ModComponents.CURRENCY.get(player);
        long wallet = cc.getValue();

        VSSkirmish.LOGGER.info("[SKIRMISH] Wager: {} gold ({} raw)", wagerInt, wager);
        VSSkirmish.LOGGER.info("[SKIRMISH] Player {} wallet: {}", player.getGameProfile().getName(), wallet);

        if (wallet < wager) {
            VSSkirmish.LOGGER.warn("[SKIRMISH] Player {} does not have enough funds. Needed {}, had {}.",
                    player.getGameProfile().getName(), wager, wallet);

            player.sendMessage(Text.literal("You don't have enough gold in your wallet for that wager!"));
            return 0;
        }

        cc.modify(-wager);
        VSSkirmish.LOGGER.info("[SKIRMISH] Deducted {} from challenger wallet. New balance: {}", wager, cc.getValue());

        // CREATE CHALLENGE
        VSSkirmish.LOGGER.info("[SKIRMISH] Creating SkirmishChallenge...");

        SkirmishChallenge challenge = new SkirmishChallenge(
                party.getId(),
                player.getUuid(),
                ship.get(),
                oppParty.getId(),
                oppLeader.getUuid(),
                oppShip.get(),
                wagerInt
        );

        sm.setCurrentChallenge(challenge);

        VSSkirmish.LOGGER.info("[SKIRMISH] Challenge created successfully.");
        VSSkirmish.LOGGER.info("[SKIRMISH] Challenger Party = {}", party.getId());
        VSSkirmish.LOGGER.info("[SKIRMISH] Opponent Party = {}", oppParty.getId());
        VSSkirmish.LOGGER.info("[SKIRMISH] Wager = {} gold", wagerInt);

        // ALERT OPPONENT PARTY
        String chPartyName = pc.getLoadedConfig(oppLeader.getUuid()).getEffective(PlayerConfigOptions.PARTY_NAME);
        if (chPartyName.isEmpty()) {
            chPartyName = party.getDefaultName();
        }

        VSSkirmish.LOGGER.info("[SKIRMISH] Resolved challenger display name: {}", chPartyName);

        String finalChPartyName = chPartyName;
        oppParty.getOnlineMemberStream().forEach(p ->
                p.sendMessage(Text.literal("Your party has been challenged to a duel by " + finalChPartyName +
                        "! With a wager of " + wagerInt + " gold! Party leader, use /skirmish accept or /skirmish deny."))
        );

        // ALERT CHALLENGER PARTY
        party.getOnlineMemberStream().forEach(p ->
                p.sendMessage(Text.literal("§eChallenging §6" + teamName + "§e to a skirmish..."))
        );

        VSSkirmish.LOGGER.info("[SKIRMISH] ===== Challenge Successfully Sent =====");
        return 1;
    }
}
