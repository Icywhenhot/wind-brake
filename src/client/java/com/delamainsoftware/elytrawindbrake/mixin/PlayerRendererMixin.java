package com.delamainsoftware.elytrawindbrake.mixin;

import com.delamainsoftware.elytrawindbrake.ClimbingPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tilts the rendered player MODEL to follow the creative climb arc, without moving the
 * camera. While gliding, vanilla poses the elytra body from {@code getViewXRot} inside
 * {@code setupRotations} (the {@code i * (-90 - pitch)} line). We redirect ONLY that one
 * call: when the local player is climbing we feed it the arc's pitch instead, so the
 * body sweeps smoothly to vertical and faces the direction of travel. The camera reads
 * {@code getViewXRot} from a different method ({@code Camera.setup}), so it stays free.
 *
 * PER-BRANCH NOTE: like the other mixin this is written against 1.21. The renderer was
 * refactored to use render states in 1.21.2+, so this redirect would need revisiting on
 * a branch that compiles against those versions.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Redirect(
            method = "setupRotations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getViewXRot(F)F"
            )
    )
    private float elytrawindbrake$modelClimbPitch(AbstractClientPlayer player, float partialTick) {
        if (player instanceof ClimbingPlayer cp && cp.elytrawindbrake$isClimbing()) {
            return cp.elytrawindbrake$getModelPitch(partialTick);
        }
        return player.getViewXRot(partialTick);
    }
}
