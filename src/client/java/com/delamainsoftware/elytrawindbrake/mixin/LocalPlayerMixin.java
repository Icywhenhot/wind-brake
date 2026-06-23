package com.delamainsoftware.elytrawindbrake.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two elytra tweaks, both client-side and both applied at the end of the local
 * player's tick:
 *
 *  1. Air-brake. While gliding with an elytra and holding the Sneak key, bleed
 *     off horizontal speed each tick. Vertical motion is left untouched so you
 *     keep gliding/descending normally — you just slow down.
 *
 *  2. Creative climb. While gliding with an elytra IN CREATIVE MODE and holding
 *     the Jump key, drive yourself straight up at a fixed rate without shedding
 *     horizontal momentum. This mirrors Bedrock's creative elytra flight: tap
 *     space and you rocket upward while keeping your speed.
 *
 * Everything referenced here is stable at the intermediary level across many
 * MC versions, which is what lets one jar span a wide version range:
 *   - LocalPlayer#tick           (the per-tick hook)
 *   - LivingEntity#isFallFlying  (gliding check)
 *   - Entity#getDeltaMovement / #setDeltaMovement (velocity)
 *   - Player#getAbilities + Abilities#instabuild  (creative-mode check)
 *   - Options#keyShift / #keyJump (the vanilla Sneak / Jump binds)
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

	// Upward velocity (blocks/tick) forced while holding Jump in creative.
	// 1.0/tick = 20 blocks/s straight up — a brisk, Bedrock-like climb. We set
	// (not add) this each tick so the ascent stays steady instead of running away.
	private static final double ELYTRAWINDBRAKE$CREATIVE_CLIMB = 1.0D;

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

	@Inject(method = "tick", at = @At("TAIL"))
	private void elytrawindbrake$creativeClimbWhileGliding(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;

		// Only while actively gliding with an elytra...
		if (!self.isFallFlying()) {
			return;
		}

		// ...and only in creative mode (instabuild is the creative-only flag).
		if (!self.getAbilities().instabuild) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.options == null || !mc.options.keyJump.isDown()) {
			return;
		}

		// Force a steady climb; leave x/z alone so horizontal speed is kept.
		Vec3 v = self.getDeltaMovement();
		self.setDeltaMovement(v.x, ELYTRAWINDBRAKE$CREATIVE_CLIMB, v.z);
	}
}
