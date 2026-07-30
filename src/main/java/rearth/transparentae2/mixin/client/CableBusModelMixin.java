package rearth.transparentae2.mixin.client;

import java.util.function.Consumer;

import net.minecraft.client.resources.model.geometry.BakedQuad;

import appeng.block.networking.CableBusRenderState;
import appeng.client.render.cablebus.CableBusModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.transparentae2.cable.CableRenderStateExtension;
import rearth.transparentae2.client.CableRenderContext;

@Mixin(CableBusModel.class)
abstract class CableBusModelMixin {
    @Inject(method = "getCableQuads", at = @At("HEAD"))
    private void transparentae2$beginCableGeometry(
            CableBusRenderState renderState,
            Consumer<BakedQuad> quadsOut,
            CallbackInfo ci) {
        CableRenderContext.begin(
                ((CableRenderStateExtension) renderState).transparentae2$isCableTreated());
    }

    @Inject(method = "getCableQuads", at = @At("RETURN"))
    private void transparentae2$endCableGeometry(
            CableBusRenderState renderState,
            Consumer<BakedQuad> quadsOut,
            CallbackInfo ci) {
        CableRenderContext.end();
    }
}
