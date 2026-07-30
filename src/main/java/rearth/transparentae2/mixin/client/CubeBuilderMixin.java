package rearth.transparentae2.mixin.client;

import net.minecraft.client.resources.model.sprite.Material;

import appeng.client.render.CubeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.transparentae2.client.CableRenderContext;

@Mixin(CubeBuilder.class)
abstract class CubeBuilderMixin {
    @Inject(method = "addCube", at = @At("HEAD"), cancellable = true)
    private void transparentae2$omitChannelOverlay(
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            CallbackInfo ci) {
        if (CableRenderContext.shouldSkipGeometry()) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "setTexture(Lnet/minecraft/client/resources/model/sprite/Material$Baked;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Material.Baked transparentae2$identifyCableTexture(Material.Baked texture) {
        return CableRenderContext.identifyCableTexture(texture);
    }

    @Inject(
            method = "setColorRGB(I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void transparentae2$disableChannelTint(int color, CallbackInfo ci) {
        if (CableRenderContext.isTreatmentActive()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "setEmissiveMaterial",
            at = @At("HEAD"),
            cancellable = true)
    private void transparentae2$disableChannelEmission(boolean renderFullBright, CallbackInfo ci) {
        if (CableRenderContext.isTreatmentActive()) {
            ci.cancel();
        }
    }
}
