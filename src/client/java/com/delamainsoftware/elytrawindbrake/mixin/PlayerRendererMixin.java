package com.delamainsoftware.elytrawindbrake.mixin;

import com.delamainsoftware.elytrawindbrake.ClimbingPlayer;
import com.delamainsoftware.elytrawindbrake.ClimbingRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tilts the rendered player MODEL to follow the creative climb arc, without moving the
 * camera — the same effect as before, adapted to the 1.21.2 entity render-state model.
 *
 * Vanilla poses the elytra body in {@code PlayerRenderer.setupRotations} with the line
 * {@code mulPose(Axis.XP.rotationDegrees(fallFlyingScale * (-90 - xRot)))}, where
 * {@code xRot} is now a FIELD on the render state instead of a {@code getViewXRot()} call.
 * Two hooks reproduce the old redirect:
 *
 *   1. {@link #elytrawindbrake$captureClimb} copies the climb flag and the interpolated
 *      model pitch off the local player into the render state during extraction (the one
 *      place the entity and the partial tick are both in hand).
 *   2. {@link #elytrawindbrake$modelClimbPitch} redirects the {@code xRot} field read
 *      inside {@code setupRotations} so, while climbing, the body sweeps to the arc pitch.
 *
 * The camera reads its pitch elsewhere (Camera.setup), and the head pose reads {@code xRot}
 * in a different method ({@code HumanoidModel.setupAnim}), so both stay untouched — the
 * mouse remains fully free in every view including F5, exactly as on the 1.21 branch.
 *
 * VERSION RANGE: this targets the render-state renderer introduced in 1.21.2 and verified
 * (against the actual obfuscated client bytecode) to apply unchanged through 1.21.8 — the
 * {@code setupRotations} signature and the {@code xRot} field read are identical across
 * that whole window. It does NOT apply to 1.21 / 1.21.1 (which still pose from a
 * {@code getViewXRot} call in LivingEntityRenderer — that's the master branch), and it
 * breaks at 1.21.10, where {@code extractRenderState}'s first parameter changed type
 * (AbstractClientPlayer -> a new render-input class), so the descriptor below no longer
 * matches. Hence fabric.mod.json caps at 1.21.8.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    // Full descriptor pins the real override, not the synthetic generic bridge of the
    // same name (which forwards to it and would mismatch this handler's signature).
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;"
                    + "Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("TAIL")
    )
    private void elytrawindbrake$captureClimb(AbstractClientPlayer player, PlayerRenderState state,
                                              float partialTick, CallbackInfo ci) {
        if (player instanceof ClimbingPlayer cp && state instanceof ClimbingRenderState crs) {
            boolean climbing = cp.elytrawindbrake$isClimbing();
            crs.elytrawindbrake$setClimb(climbing,
                    climbing ? cp.elytrawindbrake$getModelPitch(partialTick) : 0.0F);
        }
    }

    @Redirect(
            method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;xRot:F",
                    opcode = Opcodes.GETFIELD
            )
    )
    private float elytrawindbrake$modelClimbPitch(PlayerRenderState state) {
        if (state instanceof ClimbingRenderState crs && crs.elytrawindbrake$isClimbing()) {
            return crs.elytrawindbrake$getModelPitch();
        }
        return state.xRot;
    }
}
