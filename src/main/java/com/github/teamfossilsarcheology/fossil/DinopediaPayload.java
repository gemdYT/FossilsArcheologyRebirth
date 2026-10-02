package com.github.teamfossilsarcheology.fossil;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Read-only, bounded server snapshot; clients never submit care values. */
public record DinopediaPayload(String text) implements CustomPacketPayload {
    public static final Type<DinopediaPayload> TYPE = new Type<>(ModContent.id("dinopedia"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DinopediaPayload> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16384), DinopediaPayload::text, DinopediaPayload::new);
    @Override public Type<DinopediaPayload> type() { return TYPE; }
    public static String description(String species) {
        try (var stream = DinopediaPayload.class.getResourceAsStream("/assets/fossil/dinopedia/en_us/" + species + ".txt")) {
            return stream == null ? "" : new String(stream.readNBytes(10000), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) { return ""; }
    }
}
