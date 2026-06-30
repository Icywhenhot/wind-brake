package com.delamainsoftware.elytrawindbrake.mixin;

import com.delamainsoftware.elytrawindbrake.ClimbingRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds two transient fields to the player render state so the climb pose can ride along
 * with the rest of the per-frame snapshot. {@link PlayerRendererMixin} fills them in
 * during extraction and reads them back while posing the body. Nothing here references
 * the entity, so it stays a pure data carrier.
 */
@Mixin(PlayerRenderState.class)
public abstract class PlayerRenderStateMixin implements ClimbingRenderState {

    @Unique private boolean elytrawindbrake$climbing = false;
    @Unique private float elytrawindbrake$modelPitch = 0.0F;

    @Override
    public boolean elytrawindbrake$isClimbing() {
        return elytrawindbrake$climbing;
    }

    @Override
    public float elytrawindbrake$getModelPitch() {
        return elytrawindbrake$modelPitch;
    }

    @Override
    public void elytrawindbrake$setClimb(boolean climbing, float modelPitch) {
        this.elytrawindbrake$climbing = climbing;
        this.elytrawindbrake$modelPitch = modelPitch;
    }
}
