package madmike.skirmish.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import g_mungus.vlib.api.VLibGameUtils;
import madmike.skirmish.logic.SkirmishManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SkirmishCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            LiteralArgumentBuilder<ServerCommandSource> skirmishCommand = literal("skirmish")
                    // Base help command
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player != null) {
                            player.sendMessage(Text.literal("""
                                §6====== Skirmish Command Help ======

                                §e/skirmish challenge <team> §7- Challenge another team to a skirmish
                                §e/skirmish challenge <team> <wager> §7- Add an optional wager (in gold)
                                §eYou must be on the vessel of your choice when using the command
                                §eYou must be a team leader to use this command
                                
                                §e/skirmish accept §7- Accept your latest incoming challenge
                                §eYou must be on the vessel of your choice when using the command
                                §eYou must be a team leader to use this command
                                
                                §e/skirmish cancel §7- Cancel your outgoing challenge
                                §e/skirmish deny §7- Deny your latest incoming challenge
                                §e/skirmish stats §7- View your party’s stats
                                §e/skirmish top §7- View top performing captains
                                
                                §6--- Rules ---
                                
                                §7• Sink the enemy ship or eliminate all enemies to win.
                                """));
                        }
                        return 1;
                    })

                    // ============================================================
                    // /skirmish challenge <team> [wager]
                    // ============================================================
                    .then(literal("challenge")
                            .requires((source -> {
                                ServerPlayerEntity player = source.getPlayer();
                                if (player == null) return false;
                                IServerPartyAPI party = OpenPACServerAPI.get(source.getServer()).getPartyManager().getPartyByOwner(player.getUuid());
                                return party != null;
                            }))
                            .then(argument("team", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        Set<UUID> ownerIds = new HashSet<>();
                                        MinecraftServer server = context.getSource().getServer();
                                        OpenPACServerAPI api = OpenPACServerAPI.get(server);

                                        api.getPartyManager().getAllStream().forEach(t -> ownerIds.add(t.getOwner().getUUID()));
                                        for (UUID id : ownerIds) {
                                            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
                                            if (player != null) {
                                                builder.suggest(api.getPlayerConfigs().getLoadedConfig(id).getEffective(PlayerConfigOptions.PARTY_NAME));
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(SkirmishCommand::executeChallengeTeam)
                                    .then(argument("wager", IntegerArgumentType.integer(0))
                                            .executes(SkirmishCommand::executeChallengeTeam)
                                    )
                            )
                    )

                    // ============================================================
                    // /skirmish accept
                    // ============================================================
                    .then(literal("accept").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        // TODO: Accept pending challenge
                        player.sendMessage(Text.literal("§aYou accepted the latest skirmish challenge."));
                        return 1;
                    }))

                    // ============================================================
                    // /skirmish cancel
                    // ============================================================
                    .then(literal("cancel").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        // TODO: Cancel outgoing challenge
                        player.sendMessage(Text.literal("§eYou canceled your outgoing skirmish challenge."));
                        return 1;
                    }))

                    // ============================================================
                    // /skirmish deny
                    // ============================================================
                    .then(literal("deny").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        // TODO: Deny incoming challenge
                        player.sendMessage(Text.literal("§cYou denied the latest skirmish challenge."));
                        return 1;
                    }))

                    // ============================================================
                    // /skirmish stats
                    // ============================================================
                    .then(literal("stats").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        // TODO: Show player's team skirmish stats
                        player.sendMessage(Text.literal("§6[Skirmish Stats] §7Coming soon..."));
                        return 1;
                    }))

                    // ============================================================
                    // /skirmish top
                    // ============================================================
                    .then(literal("top").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        // TODO: Display leaderboard (most wins, ships sunk, gold earned)
                        player.sendMessage(Text.literal("§6[Skirmish Top] §7Leaderboard coming soon..."));
                        return 1;
                    }));

            dispatcher.register(skirmishCommand);


        });
    }

    private static int executeChallengeTeam(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("You must be a player to use this command"));
            return 0;
        }

        if (SkirmishManager.INSTANCE.hasChallengeOrSkirmish()) {
            player.sendMessage(Text.literal("There is already a skirmish or challenge, please try again later."));
            return 0;
        }

        MinecraftServer server = ctx.getSource().getServer();
        OpenPACServerAPI api = OpenPACServerAPI.get(server);

        IPartyManagerAPI pm = api.getPartyManager();
        IPlayerConfigManagerAPI pc = api.getPlayerConfigs();

        IServerPartyAPI otherParty = null;

        String teamName = StringArgumentType.getString(ctx, "team");

        Set<UUID> ownerIds = new HashSet<>();
        pm.getAllStream().forEach(t -> ownerIds.add(t.getOwner().getUUID()));
        for (UUID id : ownerIds) {
            ServerPlayerEntity otherPlayer = server.getPlayerManager().getPlayer(id);
            if (otherPlayer != null) {
                if (pc.getLoadedConfig(id).getEffective(PlayerConfigOptions.PARTY_NAME).equals(teamName)) {
                    otherParty = api.getPartyManager().getPartyByOwner(id);
                    break;
                }
            }
        }

        if (otherParty == null) {
            player.sendMessage(Text.literal("Error finding other team."));
            return 0;
        }

        VLibGameUtils.INSTANCE.saveShipToTemplate();

        VLibGameUtils.INSTANCE.getStructureTemplate()

        // TODO: Validate leader status, ensure on ship, create challenge
        player.sendMessage(Text.literal("§eChallenging team §6" + teamName + "§e to a skirmish..."));
        return 1;
    }
}
