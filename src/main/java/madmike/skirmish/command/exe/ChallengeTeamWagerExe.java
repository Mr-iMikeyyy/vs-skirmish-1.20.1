package madmike.skirmish.command.exe;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.logic.SkirmishChallenge;
import madmike.skirmish.logic.SkirmishManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.valkyrienskies.core.api.ships.Ship;
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
    public static int executeChallengeTeamWager(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("You must be a player to use this command"));
            return 0;
        }

        SkirmishManager sm = SkirmishManager.INSTANCE;
        if (sm.hasChallengeOrSkirmish()) {
            player.sendMessage(Text.literal("There is already a skirmish or pending challenge, please try again later."));
            return 0;
        }

        MinecraftServer server = ctx.getSource().getServer();
        OpenPACServerAPI api = OpenPACServerAPI.get(server);

        IPartyManagerAPI pm = api.getPartyManager();

        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.literal("You must be a party leader to use this command"));
            return 0;
        }


        StructureTemplateManager stm = player.getServerWorld().getStructureTemplateManager();
        Optional<StructureTemplate> ship = stm.getTemplate(new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId()));

        if (ship.isEmpty()) {
            player.sendMessage(Text.literal("Your party has no saved ship"));
            return 0;
        }


        IServerPartyAPI oppParty = null;

        String teamName = StringArgumentType.getString(ctx, "team");

        IPlayerConfigManagerAPI pc = api.getPlayerConfigs();

        Set<UUID> ownerIds = new HashSet<>();
        pm.getAllStream().forEach(t -> ownerIds.add(t.getOwner().getUUID()));
        for (UUID id : ownerIds) {
            ServerPlayerEntity otherPlayer = server.getPlayerManager().getPlayer(id);
            if (otherPlayer != null) {
                if (pc.getLoadedConfig(id).getEffective(PlayerConfigOptions.PARTY_NAME).equals(teamName)) {
                    oppParty = api.getPartyManager().getPartyByOwner(id);
                    break;
                }
            }
        }

        if (oppParty == null) {
            player.sendMessage(Text.literal("Error finding other team."));
            return 0;
        }

        Optional<StructureTemplate> oppShip = stm.getTemplate(new Identifier(VSSkirmish.MOD_ID, "ships/" + oppParty.getId()));

        if (oppShip.isEmpty()) {
            player.sendMessage(Text.literal("Could not find a ship for the other party"));
            return 0;
        }

        int wagerInt = IntegerArgumentType.getInteger(ctx, "wager");
        long wager = wagerInt * 10000L;
        CurrencyComponent cc = ModComponents.CURRENCY.get(player);
        long wallet = cc.getValue();

        if (wallet < wager) {
            player.sendMessage(Text.literal("You don't have enough gold in your wallet for that wager!"));
            return 0;
        }

        cc.modify(-wager);

        SkirmishChallenge challenge = new SkirmishChallenge(party.getId(), ship.get(), oppParty.getId(), wager);
        sm.setCurrentChallenge(challenge);

        String chPartyName = pc.getLoadedConfig(player.getUuid()).getEffective(PlayerConfigOptions.PARTY_NAME);

        oppParty.getOnlineMemberStream().forEach(p -> p.sendMessage(Text.literal("Your party has been challenged to a duel by " + chPartyName + "! With a wager of " + wagerInt + " gold! Party leader, use /skirmish accept or /skirmish deny.")));

        party.getOnlineMemberStream().forEach(p -> {
            p.sendMessage(Text.literal("§eChallenging §6" + teamName + "§e to a skirmish..."));
        });
        return 1;
    }
}
