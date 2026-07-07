package com.delamainsoftware.elytrawindbrake;

import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking;
import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking.CheckPayload;
import com.delamainsoftware.elytrawindbrake.net.WindBrakeNetworking.PresentPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Registers the handshake payload types (runs on both sides, so the client can send/receive
 * them too) and the server side of the handshake: answer a client's "do you have the mod?"
 * ping with a reply. A vanilla server has no receiver for {@link CheckPayload} and simply
 * drops the unknown payload, so it never sends {@link PresentPayload} and the client keeps
 * the features locked.
 *
 * The {@code main} entrypoint runs on a dedicated server AND on the integrated server behind
 * singleplayer, so singleplayer still unlocks the mod.
 */
public class WindBrakeServer implements ModInitializer {
    @Override
    public void onInitialize() {
        // Both payload types must be known on both sides: the client sends CHECK + receives
        // PRESENT, the server receives CHECK + sends PRESENT.
        PayloadTypeRegistry.playC2S().register(CheckPayload.TYPE, CheckPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PresentPayload.TYPE, PresentPayload.CODEC);

        // New-API payload receivers run on the game thread, so we can reply straight away.
        ServerPlayNetworking.registerGlobalReceiver(CheckPayload.TYPE, (payload, context) ->
                ServerPlayNetworking.send(context.player(), PresentPayload.INSTANCE));
    }
}
