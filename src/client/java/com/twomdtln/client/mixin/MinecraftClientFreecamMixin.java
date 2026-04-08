package com.twomdtln.client.mixin;

import com.twomdtln.client.ConduitFreecam;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientFreecamMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void conduit$cancelAttackDuringFreecam(CallbackInfoReturnable<Boolean> cir) {
        if (ConduitFreecam.INSTANCE.isEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void conduit$cancelUseDuringFreecam(CallbackInfo ci) {
        if (ConduitFreecam.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
