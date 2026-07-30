package rearth.transparentae2.init;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.transparentae2.TransparentAE2;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(
            Registries.DATA_COMPONENT_TYPE,
            TransparentAE2.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> CABLE_TREATED =
            COMPONENTS.registerComponentType(
                    "cable_treated",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private ModDataComponents() {
    }
}
