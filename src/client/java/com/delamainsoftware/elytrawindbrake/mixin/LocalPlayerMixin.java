package com.delamainsoftware.elytrawindbrake.mixin;

import com.delamainsoftware.elytrawindbrake.ClimbingPlayer;
import com.delamainsoftware.elytrawindbrake.config.WindBrakeConfig;
import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two elytra tweaks, both client-side and both applied at the end of the local
 * player's tick. All numbers come from {@link WindBrakeConfig} (config/elytrawindbrake.json,
 * editable in-game via Mod Menu).
 *
 *  1. Air-brake. While gliding with an elytra and holding the Sneak key, bleed
 *     off horizontal speed each tick. Vertical motion is left untouched.
 *
 *  2. Creative climb. While gliding with an elytra IN CREATIVE MODE and holding
 *     the Jump key:
 *       - nothing happens for the first {@code climbStartDelaySeconds} (the launch wind-up);
 *       - then upward speed ramps up EXPONENTIALLY each tick, capped at {@code maxClimbSpeed};
 *       - meanwhile a constant {@code forwardSpeed} is kept in the facing direction.
 *     Constant forward + exponential up traces an exponential-curve trajectory.
 *     Optionally the player is tilted to look straight up as it rockets away.
 *
 * This is the 1.20 branch: the jar is compiled against 1.20 (Java 17 bytecode) and verified
 * to run unchanged on 1.20 through 1.21.1. Everything referenced here is stable at the
 * intermediary level across that whole window, which is what lets one jar span it:
 *   - LocalPlayer#tick           (the per-tick hook)
 *   - LivingEntity#isFallFlying  (gliding check)
 *   - Entity#getDeltaMovement / #setDeltaMovement (velocity)
 *   - Entity#getYRot / #getXRot                   (orientation)
 *   - Player#getAbilities + Abilities#instabuild  (creative-mode check)
 *   - Options#keyShift / #keyJump (the vanilla Sneak / Jump binds)
 *
 * The range stops at 1.21.1 because the player renderer was rewritten to render-states in
 * 1.21.2 (see PlayerRendererMixin), so the model-tilt hook no longer applies there.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements ClimbingPlayer {

    // How many consecutive ticks Jump has been held while gliding in creative.
    // Resets to 0 the moment any of those conditions stops being true, so the
    // launch delay restarts on every fresh press.
    private int elytrawindbrake$jumpHeldTicks = 0;

    // Horizontal speed captured at the instant the arc begins, so the climb starts
    // from your current momentum instead of jolting to a fixed value.
    private double elytrawindbrake$climbBaseSpeed = 0.0D;

    // Heading (yaw) captured at the instant the arc begins. The whole climb follows
    // this fixed direction so the mouse never steers it — that's what frees the camera.
    private float elytrawindbrake$climbYaw = 0.0F;

    // Model-pose state read by the player renderer (NOT the camera). modelPitch is this
    // tick's target body pitch (-arc angle); modelPitchPrev is last tick's, so the
    // renderer can interpolate across partial ticks for a smooth tilt to vertical.
    @Unique private boolean elytrawindbrake$climbing = false;
    @Unique private float elytrawindbrake$modelPitch = 0.0F;
    @Unique private float elytrawindbrake$modelPitchPrev = 0.0F;
    // Counts ticks spent easing the model pose back to the camera after release.
    @Unique private int elytrawindbrake$returnTicks = 0;

    // How fast the model eases back to the live look on release (fraction closed per
    // tick) and a hard stop so it can't chase a moving camera forever.
    private static final float ELYTRAWINDBRAKE$MODEL_RETURN_FACTOR = 0.2F;
    private static final int ELYTRAWINDBRAKE$MODEL_RETURN_MAX_TICKS = 60;

    @Override
    public boolean elytrawindbrake$isClimbing() {
        return elytrawindbrake$climbing;
    }

    @Override
    public float elytrawindbrake$getModelPitch(float partialTick) {
        return elytrawindbrake$modelPitchPrev
                + (elytrawindbrake$modelPitch - elytrawindbrake$modelPitchPrev) * partialTick;
    }

    // After the climb ends, glide the model pose from wherever it was (often vertical)
    // back to the player's real look pitch over a few ticks, then hand control back to
    // vanilla. Without this the body snaps in a single tick, which reads as a jerk.
    @Unique
    private void elytrawindbrake$easeModelBack(float realPitch) {
        if (!elytrawindbrake$climbing) {
            return; // model already following the camera — nothing to ease
        }
        elytrawindbrake$modelPitchPrev = elytrawindbrake$modelPitch;
        elytrawindbrake$modelPitch += (realPitch - elytrawindbrake$modelPitch) * ELYTRAWINDBRAKE$MODEL_RETURN_FACTOR;
        if (Math.abs(realPitch - elytrawindbrake$modelPitch) <= 1.0F
                || ++elytrawindbrake$returnTicks > ELYTRAWINDBRAKE$MODEL_RETURN_MAX_TICKS) {
            elytrawindbrake$climbing = false;
            elytrawindbrake$returnTicks = 0;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void elytrawindbrake$brakeWhileGliding(CallbackInfo ci) {
        // Locked unless the server confirmed it has the mod (see WindBrakeClient handshake).
        if (!WindBrakeNetworking.serverHasMod()) {
            return;
        }
        WindBrakeConfig cfg = WindBrakeConfig.get();
        if (!cfg.airBrakeEnabled) {
            return;
        }

        LocalPlayer self = (LocalPlayer) (Object) this;
        if (!self.isFallFlying()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || !mc.options.keyShift.isDown()) {
            return;
        }

        Vec3 v = self.getDeltaMovement();
        self.setDeltaMovement(v.x * cfg.brakeFactor, v.y, v.z * cfg.brakeFactor);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void elytrawindbrake$creativeClimbWhileGliding(CallbackInfo ci) {
        // Locked unless the server confirmed it has the mod (see WindBrakeClient handshake).
        if (!WindBrakeNetworking.serverHasMod()) {
            return;
        }
        WindBrakeConfig cfg = WindBrakeConfig.get();
        LocalPlayer self = (LocalPlayer) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        boolean active = cfg.creativeClimbEnabled
                && self.isFallFlying()
                && self.getAbilities().instabuild
                && mc.options != null
                && mc.options.keyJump.isDown();

        if (!active) {
            elytrawindbrake$jumpHeldTicks = 0;
            elytrawindbrake$easeModelBack(self.getXRot());
            return;
        }

        elytrawindbrake$jumpHeldTicks++;

        // Launch wind-up: hold for climbStartDelaySeconds before anything happens.
        int delayTicks = (int) Math.round(cfg.climbStartDelaySeconds * 20.0D);
        if (elytrawindbrake$jumpHeldTicks <= delayTicks) {
            elytrawindbrake$easeModelBack(self.getXRot());
            return;
        }

        // climbTicks starts at 0 on the first tick past the delay.
        int climbTicks = elytrawindbrake$jumpHeldTicks - delayTicks - 1;

        // On the very first climb tick, lock in the heading and seed the speed from
        // current momentum. From here the climb runs on its own fixed line, so the
        // mouse is free to look anywhere without steering it or fighting the camera.
        if (climbTicks == 0) {
            Vec3 cur = self.getDeltaMovement();
            double horiz = Math.sqrt(cur.x * cur.x + cur.z * cur.z);
            elytrawindbrake$climbBaseSpeed = Math.max(cfg.forwardSpeed, horiz);
            elytrawindbrake$climbYaw = self.getYRot();
        }

        // Total speed ramps up exponentially each tick, capped so it stays sane.
        double speed = elytrawindbrake$climbBaseSpeed * Math.pow(cfg.climbAcceleration, climbTicks);
        if (speed > cfg.maxClimbSpeed) {
            speed = cfg.maxClimbSpeed;
        }

        // Climb angle sweeps 0 deg -> 90 deg along an exponential-approach curve:
        // starts horizontal, curves up, and asymptotes to straight-up. tau is set so
        // it's ~95% of the way vertical after secondsToVertical.
        double tau = Math.max(1.0D, cfg.secondsToVertical * 20.0D / 3.0D);
        double frac = 1.0D - Math.exp(-climbTicks / tau);
        double thetaDeg = 90.0D * frac;
        double thetaRad = Math.toRadians(thetaDeg);

        // Split the speed along that angle: shrinking horizontal, growing vertical.
        // Direction uses the LOCKED heading, not the live look, so the mouse can't steer.
        double horizSpeed = speed * Math.cos(thetaRad);
        double vy = speed * Math.sin(thetaRad);

        double yaw = Math.toRadians(elytrawindbrake$climbYaw);
        double vx = -Math.sin(yaw) * horizSpeed;
        double vz = Math.cos(yaw) * horizSpeed;
        self.setDeltaMovement(vx, vy, vz);

        // Tilt the rendered MODEL along the arc (pitch -arc angle, matching getViewXRot's
        // convention where -90 = straight up). The renderer reads this; the camera/look
        // stays untouched, so the mouse remains fully free in every view including F5.
        float newPitch = (float) -thetaDeg;
        elytrawindbrake$modelPitchPrev = (climbTicks == 0) ? newPitch : elytrawindbrake$modelPitch;
        elytrawindbrake$modelPitch = newPitch;
        elytrawindbrake$climbing = true;
        elytrawindbrake$returnTicks = 0;
    }
}
