package com.delamainsoftware.elytrawindbrake.mixin;

import com.delamainsoftware.elytrawindbrake.ClimbingPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tilts the rendered player MODEL to follow the creative climb arc, without moving the
 * camera. While gliding, vanilla poses the elytra body in {@code PlayerRenderer.setupRotations}
 * from the player's render pitch, in the {@code k * (-90 - pitch)} line. We redirect ONLY
 * that pitch read: when the local player is climbing we feed the arc's pitch instead, so the
 * body sweeps smoothly to vertical and faces the direction of travel. The camera reads its
 * pitch elsewhere ({@code Camera.setup}), so it stays free — the mouse remains fully usable.
 *
 * MULTIVERSION NOTE: across this jar's range the vanilla pitch read changed name:
 *   - 1.20.x  uses {@code AbstractClientPlayer.getXRot()}      (no-arg, current-tick pitch);
 *   - 1.21.x  uses {@code AbstractClientPlayer.getViewXRot(f)} (partial-tick interpolated).
 * Both redirects below are declared with {@code require = 0}, so on any given version exactly
 * the one that matches applies and the other is harmlessly skipped — one jar covers both.
 * (The renderer was rewritten to render-states in 1.21.2, which is why the range stops at
 * 1.21.1: there {@code setupRotations} no longer reads the pitch this way at all.)
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    // 1.20.x path: getXRot() takes no partial tick, so we capture setupRotations' own
    // partialTicks param (the trailing float) to interpolate the model pitch smoothly.
    @Redirect(
            method = "setupRotations",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getXRot()F"
            )
    )
    private float elytrawindbrake$modelClimbPitchXRot(AbstractClientPlayer player,
                                                      AbstractClientPlayer self, PoseStack poseStack,
                                                      float ageInTicks, float rotationYaw, float partialTick) {
        if (player instanceof ClimbingPlayer cp && cp.elytrawindbrake$isClimbing()) {
            return cp.elytrawindbrake$getModelPitch(partialTick);
        }
        return player.getXRot();
    }

    // 1.21.x path: getViewXRot(partialTick) — the partial tick is the call's own argument.
    @Redirect(
            method = "setupRotations",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getViewXRot(F)F"
            )
    )
    private float elytrawindbrake$modelClimbPitchViewXRot(AbstractClientPlayer player, float partialTick) {
        if (player instanceof ClimbingPlayer cp && cp.elytrawindbrake$isClimbing()) {
            return cp.elytrawindbrake$getModelPitch(partialTick);
        }
        return player.getViewXRot(partialTick);
    }
}
