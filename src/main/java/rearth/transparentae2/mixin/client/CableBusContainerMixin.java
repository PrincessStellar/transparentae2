package rearth.transparentae2.mixin.client;

import net.minecraft.core.Direction;

import appeng.api.parts.IPart;
import appeng.client.render.cablebus.CableBusRenderState;
import appeng.parts.CableBusContainer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.transparentae2.cable.CableRenderStateExtension;
import rearth.transparentae2.cable.CableTreatmentState;

@Mixin(CableBusContainer.class)
abstract class CableBusContainerMixin {
    @Shadow
    public abstract IPart getPart(@Nullable Direction side);

    @Inject(method = "getRenderState", at = @At("RETURN"))
    private void transparentae2$addCableTreatmentToRenderState(
            CallbackInfoReturnable<CableBusRenderState> cir) {
        var cable = getPart(null);
        var treated = cable instanceof CableTreatmentState treatment
                && treatment.transparentae2$isCableTreated();
        ((CableRenderStateExtension) cir.getReturnValue()).transparentae2$setCableTreated(treated);
    }
}
