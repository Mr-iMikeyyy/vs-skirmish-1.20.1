package madmike.skirmish.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import madmike.skirmish.command.exe.AcceptExe;
import madmike.skirmish.command.exe.ChallengeTeamWagerExe;
import madmike.skirmish.command.exe.SaveExe;
import madmike.skirmish.command.exe.SpectateExe;
import madmike.skirmish.command.req.PartyLeaderReq;
import madmike.skirmish.command.sug.ChallengeTeamSug;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
                                
                                §6--- Team Leaders ---
                                
                                §e/skirmish challenge <team> §7- Challenge another team to a skirmish
                                §e/skirmish challenge <team> <wager> §7- Add an optional wager (in gold)
                                §e/skirmish accept §7- Accept an incoming challenge
                                §e/skirmish cancel §7- Cancel your outgoing challenge
                                §e/skirmish deny §7- Deny an incoming challenge
                                §e/skirmish save §7- Save the ship you are standing on as your party's ship
                                
                                §6--- Players ---
                                
                                §e/skirmish stats §7- View your party’s stats
                                §e/skirmish top §7- View top performing parties
                                §e/skirmish spectate §7- Watch the current skirmish
                                
                                §6--- Rules ---
                                
                                §7• Destroy the enemy helm or eliminate all enemies to win.
                                """));
                        }
                        return 1;
                    })

                    // ============================================================
                    // /skirmish challenge <team> [wager]
                    // ============================================================
                    .then(literal("challenge")
                            .requires(PartyLeaderReq::reqPartyLeader)
                            .then(argument("team", StringArgumentType.string())
                                    .suggests(ChallengeTeamSug::sugChallengeTeam)
                                    .executes(ChallengeTeamWagerExe::executeChallengeTeamWager)
                                    .then(argument("wager", IntegerArgumentType.integer(0))
                                            .executes(ChallengeTeamWagerExe::executeChallengeTeamWager)
                                    )
                            )
                    )

                    // ============================================================
                    // /skirmish accept
                    // ============================================================
                    .then(literal("accept")
                            .requires(PartyLeaderReq::reqPartyLeader)
                            .executes(AcceptExe::executeAccept)
                    )

                    // ============================================================
                    // /skirmish cancel
                    // ============================================================
                    .then(literal("cancel")
                            .requires(PartyLeaderReq::reqPartyLeader)
                            .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        // TODO: Cancel outgoing challenge
                        player.sendMessage(Text.literal("§eYou canceled your outgoing skirmish challenge."));
                        return 1;
                    }))

                    // ============================================================
                    // /skirmish deny
                    // ============================================================
                    .then(literal("deny")
                            .requires(PartyLeaderReq::reqPartyLeader)
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                // TODO: Deny incoming challenge
                                player.sendMessage(Text.literal("§cYou denied the latest skirmish challenge."));
                                return 1;
                    }))

                    // ============================================================
                    // /skirmish save
                    // ============================================================
                    .then(literal("save")
                            .requires(PartyLeaderReq::reqPartyLeader)
                            .executes(SaveExe::executeSave)
                    )

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
                    }))

                    // ============================================================
                    // /skirmish spectate
                    // ============================================================
                    .then(literal("spectate")
                            .executes(SpectateExe::executeSpectate)
                    );

            dispatcher.register(skirmishCommand);


        });
    }


}
