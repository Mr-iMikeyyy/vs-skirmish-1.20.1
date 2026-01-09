package madmike.skirmish.command.exe;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import madmike.cc.logic.BusyPlayers;
import madmike.cc.logic.Reason;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.component.SkirmishComponents;
import madmike.skirmish.logic.SkirmishChallenge;
import madmike.skirmish.logic.SkirmishChallengeManager;
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

public class ChallengePartyWagerExe {

    public static int executeNoWager(CommandContext<ServerCommandSource> ctx) {
        return executeChallenge(
                ctx.getSource(),
                StringArgumentType.getString(ctx, "party"),
                0
        );
    }

    public static int executeWager(CommandContext<ServerCommandSource> ctx) {
        return executeChallenge(
                ctx.getSource(),
                StringArgumentType.getString(ctx, "party"),
                IntegerArgumentType.getInteger(ctx, "wager")
        );
    }


    public static int executeChallenge(ServerCommandSource src, String targetName, int wagerInt) {


        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendMessage(Text.literal("You must be a player to use this command"));
            return 0;
        }

        MinecraftServer server = src.getServer();
        OpenPACServerAPI api = OpenPACServerAPI.get(server);
        IPartyManagerAPI pm = api.getPartyManager();

        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.literal("You must be a party leader to use this command"));
            return 0;
        }

        if (!SkirmishComponents.TOGGLE.get(server.getScoreboard()).isReady(party.getId())) {
            player.sendMessage(Text.literal("Your party has not enabled skirmishes, use /skirmish toggleOn to turn on skirmishes for your party."));
            return 0;
        }

        Set<UUID> busyCh = new HashSet<>();

        party.getOnlineMemberStream().forEach( p -> {
            if (BusyPlayers.isBusy(p.getUuid())) {
                busyCh.add(player.getUuid());
            }
        });

        if (!busyCh.isEmpty()) {
            player.sendMessage(Text.literal("One or more members of your party are busy doing something else"));
            return 0;
        }

        StructureTemplateManager stm = player.getServerWorld().getStructureTemplateManager();
        Identifier selfShipID = new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId());
        Optional<StructureTemplate> ship = stm.getTemplate(selfShipID);

        if (ship.isEmpty()) {
            player.sendMessage(Text.literal("Your party has no saved ship"));
            return 0;
        }

        IPlayerConfigManagerAPI pc = api.getPlayerConfigs();

        // Collect all party owners
        Set<UUID> ownerIds = new HashSet<>();
        pm.getAllStream().forEach(t -> ownerIds.add(t.getOwner().getUUID()));

        IServerPartyAPI oppParty = null;
        ServerPlayerEntity oppLeader = null;

        for (UUID id : ownerIds) {
            ServerPlayerEntity otherPlayer = server.getPlayerManager().getPlayer(id);
            if (otherPlayer != null) {
                String partyName = pc.getLoadedConfig(id).getEffective(PlayerConfigOptions.PARTY_NAME);
                if (partyName.isEmpty()) {
                    partyName = party.getDefaultName();
                }

                if (partyName.equals(targetName)) {
                    oppParty = api.getPartyManager().getPartyByOwner(id);
                    oppLeader = otherPlayer;
                    break;
                }
            }
        }

        if (oppParty == null) {
            player.sendMessage(Text.literal("Error finding other team."));
            return 0;
        }

        Set<UUID> busyOpp = new HashSet<>();

        party.getOnlineMemberStream().forEach(p -> {
            if (BusyPlayers.isBusy(player.getUuid())) {
                busyOpp.add(player.getUuid());
            }
        });

        if (!busyOpp.isEmpty()) {
            player.sendMessage(Text.literal("One or more members of the other party are busy doing something else"));
            return 0;
        }

        if (!SkirmishComponents.TOGGLE.get(server.getScoreboard()).isReady(oppParty.getId())) {
            player.sendMessage(Text.literal("Opponent party has not yet enabled skirmishes"));
            return 0;
        }

        Identifier oppShipID = new Identifier(VSSkirmish.MOD_ID, "ships/" + oppParty.getId());
        Optional<StructureTemplate> oppShip = stm.getTemplate(oppShipID);

        if (oppShip.isEmpty()) {
            player.sendMessage(Text.literal("Could not find a ship for the other party"));
            return 0;
        }

        // WAGER CHECK
        if (wagerInt > 0) {
            long wager = wagerInt * 10000L;

            CurrencyComponent cc = ModComponents.CURRENCY.get(player);
            long wallet = cc.getValue();

            if (wallet < wager) {

                player.sendMessage(Text.literal("You don't have enough gold in your wallet for that wager!"));
                return 0;
            }
        }


        // CREATE CHALLENGE

        SkirmishChallenge challenge = new SkirmishChallenge(
                party.getId(),
                ship.get(),
                oppParty.getId(),
                oppShip.get(),
                wagerInt
        );

        SkirmishChallengeManager.INSTANCE.addChallenge(challenge);

        // ALERT OPPONENT PARTY
        String chPartyName = pc.getLoadedConfig(oppLeader.getUuid()).getEffective(PlayerConfigOptions.PARTY_NAME);
        if (chPartyName.isEmpty()) {
            chPartyName = party.getDefaultName();
        }

        String finalChPartyName = chPartyName;
        oppParty.getOnlineMemberStream().forEach(p -> {
                p.sendMessage(Text.literal("Your party has been challenged to a duel by " + finalChPartyName +
                        "! With a wager of " + wagerInt + " gold! Party leader, use /skirmish accept or /skirmish deny.")
                );
                BusyPlayers.add(p.getUuid(), Reason.SKIRMISH);
            }
        );

        // ALERT CHALLENGER PARTY
        party.getOnlineMemberStream().forEach(p -> {
                p.sendMessage(Text.literal("§eChallenging §6" + targetName + "§e to a skirmish..."));
                BusyPlayers.add(p.getUuid(), Reason.SKIRMISH);
            }
        );

        VSSkirmish.LOGGER.info("[SKIRMISH] ===== Challenge Successfully Sent =====");
        return 1;
    }
}
