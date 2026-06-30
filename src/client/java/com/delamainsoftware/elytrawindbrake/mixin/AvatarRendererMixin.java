package com.delamainsoftware.elytrawindbrake.mixin;

import com.delamainsoftware.elytrawindbrake.ClimbingPlayer;
import com.delamainsoftware.elytrawindbrake.ClimbingRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tilts the rendered player MODEL to follow the creative climb arc, without moving the
 * camera.
 *
 * Vanilla poses the elytra body in {@code AvatarRenderer.setupRotations} with the line
 * {@code mulPose(Axis.XP.rotationDegrees(fallFlyingScale * (-90 - xRot)))}, where {@code xRot}
 * is read as a FIELD off the render state. Two hooks reproduce that pose from the climb arc:
 *
 *   1. {@link #elytrawindbrake$captureClimb} copies the climb flag and the interpolated
 *      model pitch off the local player into the render state during extraction (the one
 *      place the entity and the partial tick are both in hand).
 *   2. {@link #elytrawindbrake$modelClimbPitch} redirects the {@code xRot} field read inside
 *      {@code setupRotations} so, while climbing, the body sweeps to the arc pitch.
 *
 * The camera reads its pitch elsewhere, and the head pose reads {@code xRot} in a different
 * method ({@code HumanoidModel.setupAnim}), so both stay untouched — the mouse stays fully
 * free in every view including F5.
 *
 * 26.1+ is unobfuscated, so the names below ARE the runtime names (no remap). Both injectors
 * are pinned with full descriptors to dodge the synthetic generic bridges of the same name.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
                    + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void elytrawindbrake$captureClimb(Avatar avatar, AvatarRenderState state,
                                              float partialTick, CallbackInfo ci) {
        if (avatar instanceof ClimbingPlayer cp && state instanceof ClimbingRenderState crs) {
            boolean climbing = cp.elytrawindbrake$isClimbing();
            crs.elytrawindbrake$setClimb(climbing,
                    climbing ? cp.elytrawindbrake$getModelPitch(partialTick) : 0.0F);
        }
    }

    @Redirect(
            method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;xRot:F",
                    opcode = Opcodes.GETFIELD
            )
    )
    private float elytrawindbrake$modelClimbPitch(AvatarRenderState state) {
        if (state instanceof ClimbingRenderState crs && crs.elytrawindbrake$isClimbing()) {
            return crs.elytrawindbrake$getModelPitch();
        }
        return state.xRot;
    }
}
