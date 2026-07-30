package rearth.transparentae2.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_RENDER_TRANSFER_PATHS = BUILDER
            .comment("Draw the route line followed by moving AE2 transfer items.")
            .define("debugRenderTransferPaths", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
