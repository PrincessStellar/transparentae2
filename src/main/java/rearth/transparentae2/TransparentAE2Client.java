package rearth.transparentae2;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import rearth.transparentae2.client.ClientConfig;
import rearth.transparentae2.client.ClientTransferPathRenderer;
import rearth.transparentae2.network.TransferPathNetworking;

@Mod(value = TransparentAE2.MODID, dist = Dist.CLIENT)
public final class TransparentAE2Client {
    public TransparentAE2Client(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        TransferPathNetworking.setClientHandler(ClientTransferPathRenderer::add);
        NeoForge.EVENT_BUS.addListener(ClientTransferPathRenderer::render);
        NeoForge.EVENT_BUS.addListener(this::onLogout);
    }

    private void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTransferPathRenderer.clear();
    }
}
