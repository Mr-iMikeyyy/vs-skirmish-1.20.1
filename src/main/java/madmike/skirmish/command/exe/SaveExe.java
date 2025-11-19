package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import g_mungus.vlib.api.VLibGameUtils;
import madmike.skirmish.VSSkirmish;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class SaveExe {
    public static int executeSave(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        // Get party data
        OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
        IPartyManagerAPI pm = api.getPartyManager();
        IServerPartyAPI party = pm.getPartyByOwner(player.getUuid());

        if (party == null) {
            player.sendMessage(Text.literal("§cYou are not the owner of a party."), false);
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        // Get the ship the player is currently on
        Ship ship = VSGameUtilsKt.getShipManagingPos(world, player.getBlockPos());
        if (ship == null) {
            player.sendMessage(Text.literal("§cYou are not standing on a ship."), false);
            return 0;
        }

        // Build structure save path
        String path = VSSkirmish.MOD_ID + "/ships/" + party.getId();

        // Save ship to template
        VLibGameUtils.INSTANCE.saveShipToTemplate(
                path,
                world,
                ship.getId(),
                true,   // include entities
                false   // do not delete ship after saving
        );

        player.sendMessage(Text.literal("§6[Skirmish Save] §7Saved your party ship."), false);
        return 1;
    }
}
