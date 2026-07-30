package rearth.transparentae2;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import rearth.transparentae2.network.TransferPathNetworking;

@Mod(TransparentAE2.MODID)
public class TransparentAE2 {
    public static final String MODID = "transparentae2";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TransparentAE2(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(TransferPathNetworking::register);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("Transparent AE2 transfer logging prototype enabled");
    }
}
