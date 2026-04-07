package com.promocodemod.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class PromoCodeConfig {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ServerConfig SERVER;

    static {
        final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class ServerConfig {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> promoCodes;

        ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("PromoCode Mod Configuration").push("promocodes");

            promoCodes = builder
                .comment(
                    "List of promo codes. Format: CODE:item,count;item2,count2|maxUses|expiryEpochSeconds",
                    "Example: SUMMER2024:minecraft:diamond,5;minecraft:apple,10|50|0",
                    "Set expiryEpochSeconds to 0 for no expiry."
                )
                .defineList("codes", Arrays.asList(), obj -> obj instanceof String);

            builder.pop();
        }
    }
}
