package rearth.transparentae2;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_ITEM_TRANSFERS = BUILDER
            .comment("Log successful AE2 item transfers. Intended for prototype validation.")
            .define("logItemTransfers", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
