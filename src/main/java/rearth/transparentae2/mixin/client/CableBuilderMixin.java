package rearth.transparentae2.mixin.client;

import java.util.EnumMap;

import net.minecraft.client.resources.model.sprite.Material;

import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.block.networking.CableCoreType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.transparentae2.client.CableRenderContext;

@Mixin(targets = "appeng.client.render.cablebus.CableBuilder")
abstract class CableBuilderMixin {
    @Shadow
    @Final
    private EnumMap<CableCoreType, EnumMap<AEColor, Material.Baked>> coreTextures;

    @Shadow
    @Final
    private EnumMap<AECableType, EnumMap<AEColor, Material.Baked>> connectionTextures;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void transparentae2$registerCableTextures(CallbackInfo ci) {
        CableRenderContext.registerCableTextures(coreTextures, connectionTextures);
    }
}
