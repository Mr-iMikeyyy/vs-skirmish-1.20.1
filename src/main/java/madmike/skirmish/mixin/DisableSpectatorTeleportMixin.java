package madmike.skirmish.mixin;

import net.minecraft.network.packet.c2s.play.SpectatorTeleportC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class DisableSpectatorTeleportMixin {

    @Inject(
            at = @At("HEAD"),
            method = "onSpectatorTeleport",
            cancellable = true
    )
    private void skirmish$blockSpectatorHotkeyTeleport(
            SpectatorTeleportC2SPacket packet, CallbackInfo ci
    ) {
        ci.cancel();
    }
}
