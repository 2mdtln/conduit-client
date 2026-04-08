package com.twomdtln.client.mixin;

import com.twomdtln.client.ConduitFreecam;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityFreecamMixin {
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void conduit$cancelMovementPacketsDuringFreecam(CallbackInfo ci) {
        if (ConduitFreecam.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
