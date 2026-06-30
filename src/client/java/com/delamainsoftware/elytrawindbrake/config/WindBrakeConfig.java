package com.delamainsoftware.elytrawindbrake.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain-data config, serialized to {@code config/elytrawindbrake.json} with Gson
 * (Gson ships with Minecraft, so this needs no extra dependency). Loaded lazily on
 * first access — there is no client initializer — and re-saved whenever the Mod Menu
 * screen closes. Every field is also clamped on load/save so a hand-edited file can't
 * push values into nonsense ranges.
 */
public class WindBrakeConfig {

    // ---- Air brake: hold Sneak while gliding to bleed off horizontal speed ----
    public boolean airBrakeEnabled = true;
    /** Horizontal velocity KEPT per tick while braking (lower = stronger brake). */
    public double brakeFactor = 0.92D;

    // ---- Creative climb: hold Jump while gliding in creative ----
    // The climb is driven by an angle that sweeps from horizontal (0 deg) up to
    // vertical (90 deg). Speed is split along that angle, so you start by moving
    // forward, curve smoothly upward, and end up going straight up — a real arc,
    // not an L. Total speed ramps up exponentially the whole time, up to a cap.
    public boolean creativeClimbEnabled = true;
    /** Seconds to wait after pressing Jump before the climb actually starts. */
    public double climbStartDelaySeconds = 1.0D;
    /** Horizontal speed (blocks/tick) the arc starts at — the cruise speed before
     *  it tips upward. Your current glide speed is used instead if it's faster. */
    public double forwardSpeed = 0.5D;
    /** How long (seconds) the arc takes to become (nearly) vertical. Smaller = the
     *  curve whips up faster; larger = a long, lazy arc. */
    public double secondsToVertical = 5.0D;
    /** Exponential growth of total speed per tick while held (>1). */
    public double climbAcceleration = 1.04D;
    /** Hard cap on total speed (blocks/tick) so it never gets TOO fast. */
    public double maxClimbSpeed = 1.5D;

    // ---------------------------------------------------------------------------

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("elytrawindbrake.json");

    private static WindBrakeConfig instance;

    public static WindBrakeConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static WindBrakeConfig load() {
        WindBrakeConfig cfg;
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                cfg = GSON.fromJson(r, WindBrakeConfig.class);
                if (cfg == null) {
                    cfg = new WindBrakeConfig();
                }
            } catch (IOException | RuntimeException e) {
                cfg = new WindBrakeConfig();
            }
        } else {
            cfg = new WindBrakeConfig();
        }
        instance = cfg;
        cfg.save(); // normalizes + writes a freshly-created file with all keys present
        return cfg;
    }

    public void save() {
        clamp();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, w);
            }
        } catch (IOException ignored) {
            // Non-fatal: fall back to in-memory values for this session.
        }
    }

    private void clamp() {
        brakeFactor = clampD(brakeFactor, 0.0D, 1.0D);
        climbStartDelaySeconds = clampD(climbStartDelaySeconds, 0.0D, 10.0D);
        forwardSpeed = clampD(forwardSpeed, 0.0D, 5.0D);
        secondsToVertical = clampD(secondsToVertical, 1.0D, 10.0D);
        climbAcceleration = clampD(climbAcceleration, 1.0D, 2.0D);
        maxClimbSpeed = clampD(maxClimbSpeed, 0.05D, 5.0D);
    }

    private static double clampD(double v, double lo, double hi) {
        if (v < lo) return lo;
        return Math.min(v, hi);
    }
}
