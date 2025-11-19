package madmike.skirmish.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import g_mungus.vlib.api.VLibGameUtils;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.command.exe.AcceptExe;
import madmike.skirmish.command.exe.ChallengeTeamWagerExe;
import madmike.skirmish.command.exe.SaveExe;
import madmike.skirmish.command.req.PartyLeaderReq;
import madmike.skirmish.logic.SkirmishChallenge;
import madmike.skirmish.logic.SkirmishManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.world.ServerShipWorld;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

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
                            .requires(PartyLeaderReq::reqPartyLeader)
                            .then(argument("team", StringArgumentType.string())
                                    .suggests(Suggester::sugChallengeTeam)
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
                    }))

                    // ============================================================
                    // /skirmish save
                    // ============================================================
                    .then(literal("save")
                            .requires(PartyLeaderReq::reqPartyLeader)
                            .executes(SaveExe::executeSave)
                    );



            dispatcher.register(skirmishCommand);


        });
    }


}
