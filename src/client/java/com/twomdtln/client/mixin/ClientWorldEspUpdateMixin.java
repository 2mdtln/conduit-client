package com.twomdtln.client.mixin;

import com.twomdtln.client.ConduitEspRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldEspUpdateMixin {
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void conduit$invalidateEspChunk(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        ConduitEspRenderer.INSTANCE.onBlockUpdate(pos);
    }
}
