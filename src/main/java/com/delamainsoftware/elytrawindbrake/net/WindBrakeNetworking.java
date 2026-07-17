package com.delamainsoftware.elytrawindbrake.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Shared handshake payloads + the one flag that gates every feature.
 *
 * The mod refuses to do anything unless the SERVER it's connected to also has the mod.
 * The handshake is a ping/pong of two empty custom payloads:
 *   - the client fires {@link CheckPayload} at the server the moment it joins;
 *   - a server that has this mod answers with {@link PresentPayload} (a vanilla server can't,
 *     so it never does), and only that answer flips {@link #serverHasMod} on.
 *
 * This branch targets 1.21.11 ONLY (compiled against it). It uses Minecraft's
 * {@code CustomPacketPayload} codec networking; both payloads are empty — their mere arrival
 * is the entire message — so each uses {@link StreamCodec#unit}. NOTE: a jar does NOT span
 * 1.21.9–1.21.11 — MC renamed {@code ResourceLocation}→{@code Identifier} and the CycleButton
 * builder's intermediary id drifted within that window, so each release needs its own build.
 *
 * Lives in the {@code main} source set so both the server initializer (main) and the
 * client initializer/mixin (client) can reference it — the client source set sees main.
 */
public final class WindBrakeNetworking {
    private WindBrakeNetworking() {}

    /** Serverbound: "do you have the mod?" — the client's join-time ping. */
    public record CheckPayload() implements CustomPacketPayload {
        public static final CheckPayload INSTANCE = new CheckPayload();
        public static final Type<CheckPayload> TYPE = new Type<>(id("check"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CheckPayload> CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<CheckPayload> type() {
            return TYPE;
        }
    }

    /** Clientbound: the server's "yes, I do" reply — the only thing that unlocks the mod. */
    public record PresentPayload() implements CustomPacketPayload {
        public static final PresentPayload INSTANCE = new PresentPayload();
        public static final Type<PresentPayload> TYPE = new Type<>(id("present"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PresentPayload> CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<PresentPayload> type() {
            return TYPE;
        }
    }

    // Identifier's constructors are private, so build IDs via tryParse(String). NOTE: MC renamed
    // ResourceLocation -> Identifier between 1.21.9 and 1.21.11, so this branch (compiled against
    // 1.21.11) uses Identifier — a 1.21.9-compiled jar will NOT run on 1.21.11.
    private static Identifier id(String path) {
        return Identifier.tryParse("elytrawindbrake:" + path);
    }

    // Written on the client's network/tick thread (packet receive / connection events), read on
    // the client tick + render threads, hence volatile. Defaults to false, so the mod starts
    // locked on every connection and only the server's reply can unlock it.
    private static volatile boolean serverHasMod = false;

    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static void setServerHasMod(boolean value) {
        serverHasMod = value;
    }
}
