package madmike.skirmish.command.exe;

import com.mojang.brigadier.context.CommandContext;
import madmike.skirmish.VSSkirmish;
import madmike.skirmish.component.SkirmishComponents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.Optional;

public class TurnOnExe {
    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Must be a player to use this command"));
            return 0;
        }

        IServerPartyAPI party = OpenPACServerAPI.get(ctx.getSource().getServer()).getPartyManager().getPartyByOwner(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.literal("Must be the owner of a party to use this command"));
            return 0;
        }

        StructureTemplateManager stm = player.getServerWorld().getStructureTemplateManager();
        Optional<StructureTemplate> ship = stm.getTemplate(new Identifier(VSSkirmish.MOD_ID, "ships/" + party.getId()));

        if (ship.isEmpty()) {
            player.sendMessage(Text.literal("Your party has no saved ship"));
            return 0;
        }

        SkirmishComponents.TOGGLE.get(ctx.getSource().getServer().getScoreboard()).setToggleOn(party.getId());
        party.getOnlineMemberStream().forEach(p -> p.sendMessage(Text.literal("Your party has turned on skirmishes")));
        return 1;
    }
}
