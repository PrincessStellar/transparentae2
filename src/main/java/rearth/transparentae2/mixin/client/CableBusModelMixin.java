package rearth.transparentae2.mixin.client;

import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;

import appeng.client.render.cablebus.CableBusRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.transparentae2.cable.CableRenderStateExtension;
import rearth.transparentae2.client.CableRenderContext;

@Mixin(targets = "appeng.client.render.cablebus.CableBusBakedModel")
abstract class CableBusModelMixin {
    @Inject(method = "addCableQuads", at = @At("HEAD"))
    private void transparentae2$beginCableGeometry(
            CableBusRenderState renderState,
            List<BakedQuad> quadsOut,
            CallbackInfo ci) {
        CableRenderContext.begin(
                ((CableRenderStateExtension) renderState).transparentae2$isCableTreated());
    }

    @Inject(method = "addCableQuads", at = @At("RETURN"))
    private void transparentae2$endCableGeometry(
            CableBusRenderState renderState,
            List<BakedQuad> quadsOut,
            CallbackInfo ci) {
        CableRenderContext.end();
    }
}
