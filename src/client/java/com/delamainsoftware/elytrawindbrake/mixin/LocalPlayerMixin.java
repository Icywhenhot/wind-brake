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
 * Everything referenced here is stable at the intermediary level across many
 * MC versions, which is what lets one jar span a wide version range:
 *   - LocalPlayer#tick           (the per-tick hook)
 *   - LivingEntity#isFallFlying  (gliding check)
 *   - Entity#getDeltaMovement / #setDeltaMovement (velocity)
 *   - Options#keyShift           (the vanilla Sneak bind)
 *
 * PER-BRANCH NOTE: on 1.21.5+ the human-readable name of isFallFlying() became
 * isGliding(). The intermediary name (and therefore the compiled bytecode) is
 * identical, so a jar built here still runs on 1.21.5+. But if you create a
 * branch that *compiles* against 1.21.5+, rename the call below to isGliding().
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
