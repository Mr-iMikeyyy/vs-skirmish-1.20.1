package madmike.skirmish.command.sug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import madmike.skirmish.component.SkirmishComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ChallengeTeamSug {
    public static CompletableFuture<Suggestions> sugChallengeTeam(CommandContext<ServerCommandSource> src, SuggestionsBuilder builder) {

        MinecraftServer server = src.getSource().getServer();
        PlayerManager plm = server.getPlayerManager();
        OpenPACServerAPI api = OpenPACServerAPI.get(server);
        IPartyManagerAPI pm = api.getPartyManager();
        IPlayerConfigManagerAPI pc = api.getPlayerConfigs();
        Set<UUID> enabledParties = SkirmishComponents.TOGGLE.get(server.getScoreboard()).getEnabledParties();
        ServerPlayerEntity player = src.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }
        String playersPartyName = pc.getLoadedConfig(src.getSource().getPlayer().getUuid()).getEffective(PlayerConfigOptions.PARTY_NAME);

        for (UUID id : enabledParties) {

            IServerPartyAPI party = pm.getPartyById(id);
            if (party == null) {
                SkirmishComponents.TOGGLE.get(server.getScoreboard()).setToggleOff(id);
                continue;
            }

            UUID ownerId = party.getOwner().getUUID();
            ServerPlayerEntity owner = plm.getPlayer(ownerId);
            if (owner == null) {
                continue;
            }
            String partyName = pc.getLoadedConfig(ownerId).getEffective(PlayerConfigOptions.PARTY_NAME);
            if (partyName.equals(playersPartyName)) {
                continue;
            }

            builder.suggest(partyName);
        }

        return builder.buildFuture();
    }
}
