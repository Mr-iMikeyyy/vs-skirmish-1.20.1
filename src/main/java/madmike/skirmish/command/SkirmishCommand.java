package madmike.skirmish.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import madmike.skirmish.command.exe.*;
import madmike.skirmish.command.req.*;
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
                                
                                §e/skirmish challenge <party> §7- Challenge another team to a skirmish
                                §e/skirmish challenge <party> <wager> §7- Add an optional wager (in gold)
                                §e/skirmish accept §7- Accept an incoming challenge
                                §e/skirmish cancel §7- Cancel your outgoing challenge
                                §e/skirmish deny §7- Deny an incoming challenge
                                §e/skirmish save §7- Save the ship you are standing on as your party's ship
                                §e/skirmish toggleOn §7- Turn on receiving/sending challenges
                                §e/skirmish toggleOff §7- Turn off receiving/sending challenges
                                
                                §6--- Players ---
                                
                                §e/skirmish stats §7- View your party’s stats
                                §e/skirmish top §7- View top performing parties
                                §e/skirmish spectate <player> §7- Watch the current skirmish
                                
                                §6--- Rules ---
                                
                                §7• Destroy the enemy helm or eliminate all enemies to win.
                                """)
                            );
                        }
                        return 1;
                    })

                    // ============================================================
                    // /skirmish challenge <party> [wager]
                    // ============================================================
                    .then(literal("challenge")
                            .requires(NotBusyReq::require)
                            .requires(ToggleOnReq::require)
                            .then(argument("party", StringArgumentType.string())
                                    .suggests(ChallengeTeamSug::suggest)
                                    .executes(ChallengePartyWagerExe::executeNoWager)
                                    .then(argument("wager", IntegerArgumentType.integer(0))
                                            .executes(ChallengePartyWagerExe::executeWager)
                                    )
                            )
                    )

                    // ============================================================
                    // /skirmish accept
                    // ============================================================
                    .then(literal("accept")
                            .requires(HasIncomingChallengeReq::require)
                            .executes(AcceptExe::execute)
                    )

                    // ============================================================
                    // /skirmish deny
                    // ============================================================
                    .then(literal("deny")
                            .requires(HasIncomingChallengeReq::require)
                            .executes(DenyExe::execute)
                    )

                    // ============================================================
                    // /skirmish cancel
                    // ============================================================
                    .then(literal("cancel")
                            .requires(HasOutgoingChallengeReq::require)
                            .executes(CancelExe::execute)
                    )

                    // ============================================================
                    // /skirmish save
                    // ============================================================
                    .then(literal("save")
                            .requires(NotBusyReq::require)
                            .requires(PartyLeaderReq::require)
                            .executes(SaveExe::execute)
                    )

                    // ============================================================
                    // /skirmish toggle on
                    // ============================================================
                    .then(literal("toggleOn")
                            .requires(ToggleOffReq::require)
                            .executes(ToggleOnExe::execute)
                    )

                    // ============================================================
                    // /skirmish toggle off
                    // ============================================================
                    .then(literal("toggleOff")
                            .requires(ToggleOnReq::require)
                            .executes(ToggleOffExe::execute)
                    )

                    // ============================================================
                    // /skirmish stats
                    // ============================================================
                    .then(literal("stats")
                            .requires(PartyReq::require)
                            .executes(StatsExe::execute))

                    // ============================================================
                    // /skirmish top
                    // ============================================================
                    .then(literal("top")
                            .executes(TopExe::execute))

                    // ============================================================
                    // /skirmish spectate
                    // ============================================================
                    .then(literal("spectate")
                            .requires(NotBusyReq::require)
                            .requires(SkirmishOngoingReq::require)
                            .executes(SpectateExe::execute)
                    )

                    // ============================================================
                    // /skirmish spectate
                    // ============================================================
                    .then(literal("leave")
                            .requires(IsSpectatingReq::require)
                            .executes(LeaveExe::execute)
                    )

                    // ============================================================
                    // /skirmish admin
                    // ============================================================
                    .then(literal("admin")
                            .requires(source -> source.hasPermissionLevel(2))
                            // TODO DELETE SHIPS
//                            .then(literal("deleteSavedShip")
//                                    .then(argument("ship", StringArgumentType.string())
//                                            .suggests(AdminDeleteSavedShipSug::suggest)
//                                            .executes(TestExe::executeTestSpawnBasic)
//                                    )
//                            )
//                            .then(literal("deleteAllShips")
//                                    .executes(TestExe::executeTestSpawnAdvanced)
//                            )
                            // TODO DELETE SPECIFIC STAT
//                            .then(literal("resetStat")
//                                    .then(argument("party", StringArgumentType.string())
//                                            .suggests(AdminResetStatSug::suggest)
//                                            .executes(AdminResetStatExe::execute)
//                                    )
//                            )
                            .then(literal("resetAllStats")
                                    .executes(AdminResetAllStatsExe::execute)
                            )
                    );


            dispatcher.register(skirmishCommand);

        });
    }
}
