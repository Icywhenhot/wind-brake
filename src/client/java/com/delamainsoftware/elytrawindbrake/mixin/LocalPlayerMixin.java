package com.delamainsoftware.elytrawindbrake.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Elytra air-brake. While gliding with an elytra and holding the Sneak key,
 * bleed off horizontal speed each tick. Vertical motion is left untouched so
 * you keep gliding/descending normally — you just slow down.
 *
 * Minecraft 26.1+ ships UNOBFUSCATED, so this jar links directly against the real
 * Mojang names below — there is no intermediary/remap layer anymore. One jar spans
 * 26.1–26.2 simply because every member it touches keeps the same name and signature
 * across those releases (verified against the 26.1.2 client jar):
 *   - LocalPlayer#tick           (the per-tick hook)
 *   - LivingEntity#isFallFlying  (gliding check — still this name in 26.x)
 *   - Entity#getDeltaMovement / #setDeltaMovement (velocity)
 *   - Options#keyShift           (the vanilla Sneak bind)
 *
 * If a future MC release renames any of these, the mod won't crash — it just won't
 * load on that version (its fabric.mod.json mc range and the missing symbol keep it
 * out), so widen the range only after re-checking the names there.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	// Horizontal velocity multiplier applied each tick while braking.
	// ~8%/tick bleed: 0.92^20 ≈ 0.19, so ~80% of speed is gone after one second.
	private static final double ELYTRAWINDBRAKE$BRAKE_FACTOR = 0.92D;

	@Inject(method = "tick", at = @At("TAIL"))
	private void elytrawindbrake$brakeWhileGliding(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;

		// Only act while actively gliding with an elytra.
		if (!self.isFallFlying()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.options == null || !mc.options.keyShift.isDown()) {
			return;
		}

		Vec3 v = self.getDeltaMovement();
		self.setDeltaMovement(v.x * ELYTRAWINDBRAKE$BRAKE_FACTOR, v.y, v.z * ELYTRAWINDBRAKE$BRAKE_FACTOR);
	}
}
