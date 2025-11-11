package madmike.skirmish.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
                            .then(argument("team", StringArgumentType.string())
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                        String teamName = StringArgumentType.getString(ctx, "team");

                                        // TODO: Validate leader status, ensure on ship, create challenge
                                        player.sendMessage(Text.literal("§eChallenging team §6" + teamName + "§e to a skirmish..."));
                                        return 1;
                                    })
                                    .then(argument("wager", IntegerArgumentType.integer(0))
                                            .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                String teamName = StringArgumentType.getString(ctx, "team");
                                                int wager = IntegerArgumentType.getInteger(ctx, "wager");

                                                // TODO: Create skirmish challenge with wager
                                                player.sendMessage(Text.literal("§eChallenging team §6" + teamName + "§e with a wager of §6" + wager + " gold§e."));
                                                return 1;
                                            })
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
}
