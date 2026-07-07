package com.delamainsoftware.elytrawindbrake.gui;

import com.delamainsoftware.elytrawindbrake.config.WindBrakeConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

/**
 * Mod Menu config screen, built from plain vanilla widgets (toggles + sliders) so the
 * mod doesn't need Cloth Config. Edits live on the shared {@link WindBrakeConfig} and
 * are written to disk when the screen closes.
 */
public class WindBrakeConfigScreen extends Screen {

    private final Screen parent;
    private final WindBrakeConfig cfg;

    public WindBrakeConfigScreen(Screen parent) {
        super(Component.literal("Elytra Wind Brake"));
        this.parent = parent;
        this.cfg = WindBrakeConfig.get();
    }

    @Override
    protected void init() {
        final int colW = 200;
        final int gap = 6;
        final int rowH = 20;
        final int leftX = this.width / 2 - colW - gap / 2;
        final int rightX = this.width / 2 + gap / 2;
        final int top = 40;

        // Left column — toggles + the air brake.
        int yL = top;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.airBrakeEnabled)
                .create(leftX, yL, colW, rowH, Component.literal("Air Brake (Sneak)"),
                        (b, v) -> cfg.airBrakeEnabled = v));
        yL += rowH + gap;
        addRenderableWidget(new DoubleSlider(leftX, yL, colW, rowH,
                "Brake Strength", 0.50D, 0.99D, cfg.brakeFactor, v -> cfg.brakeFactor = v));
        yL += rowH + gap;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.creativeClimbEnabled)
                .create(leftX, yL, colW, rowH, Component.literal("Creative Climb (Jump)"),
                        (b, v) -> cfg.creativeClimbEnabled = v));

        // Right column — the climb numbers.
        int yR = top;
        addRenderableWidget(new DoubleSlider(rightX, yR, colW, rowH,
                "Launch Delay (sec)", 0.0D, 5.0D, cfg.climbStartDelaySeconds,
                v -> cfg.climbStartDelaySeconds = v));
        yR += rowH + gap;
        addRenderableWidget(new DoubleSlider(rightX, yR, colW, rowH,
                "Start Speed", 0.1D, 2.0D, cfg.forwardSpeed,
                v -> cfg.forwardSpeed = v));
        yR += rowH + gap;
        addRenderableWidget(new DoubleSlider(rightX, yR, colW, rowH,
                "Seconds To Vertical", 1.0D, 10.0D, cfg.secondsToVertical,
                v -> cfg.secondsToVertical = v));
        yR += rowH + gap;
        addRenderableWidget(new DoubleSlider(rightX, yR, colW, rowH,
                "Climb Acceleration", 1.0D, 1.2D, cfg.climbAcceleration,
                v -> cfg.climbAcceleration = v));
        yR += rowH + gap;
        addRenderableWidget(new DoubleSlider(rightX, yR, colW, rowH,
                "Max Climb Speed", 0.1D, 3.0D, cfg.maxClimbSpeed,
                v -> cfg.maxClimbSpeed = v));

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, rowH).build());
    }

    @Override
    public void onClose() {
        cfg.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Draw the normal menu background: the tiled dirt texture on the main-menu screens
        // and a dimmed view of the world when opened in-game. (This branch targets 1.20.2–1.20.4,
        // where renderBackground is the 4-arg overload; it's stable across that whole range, so
        // calling it directly is safe. The flat grey fill this replaced was only ever there to
        // dodge this exact 1-arg vs 4-arg signature difference.)
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        // Color is ARGB: 0xFFFFFF has alpha 0 and transparent text is skipped on newer
        // versions, so the title would be invisible. Use 0xFFFFFFFF (opaque white).
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
    }

    /** Generic slider that maps the 0..1 handle onto an arbitrary [min, max] range. */
    private static final class DoubleSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;

        DoubleSlider(int x, int y, int w, int h, String label,
                     double min, double max, double initial, DoubleConsumer setter) {
            super(x, y, w, h, Component.empty(), (initial - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private double actual() {
            return this.min + this.value * (this.max - this.min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(this.label + ": " + String.format("%.2f", actual())));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(actual());
        }
    }
}
