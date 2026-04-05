package com.twomdtln.client.mixin;

import com.twomdtln.client.ConduitClientFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFullBrightMixin {
	private static final StatusEffectInstance CONDUIT_CLIENT$FULL_BRIGHT_NIGHT_VISION =
		new StatusEffectInstance(StatusEffects.NIGHT_VISION, 220, 0, false, false, false);

	@Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
	private void conduit$forceNightVisionPresence(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
		if (effect.equals(StatusEffects.NIGHT_VISION) && conduit$isLocalPlayerFullBrightTarget()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getStatusEffect", at = @At("HEAD"), cancellable = true)
	private void conduit$forceNightVisionInstance(
		RegistryEntry<StatusEffect> effect,
		CallbackInfoReturnable<StatusEffectInstance> cir
	) {
		if (effect.equals(StatusEffects.NIGHT_VISION) && conduit$isLocalPlayerFullBrightTarget()) {
			cir.setReturnValue(CONDUIT_CLIENT$FULL_BRIGHT_NIGHT_VISION);
		}
	}

	private boolean conduit$isLocalPlayerFullBrightTarget() {
		if (!ConduitClientFeatures.INSTANCE.isFullBrightEnabled()) {
			return false;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		return client.player != null && (Object) this instanceof ClientPlayerEntity && client.player == (Object) this;
	}
}
