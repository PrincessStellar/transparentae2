package rearth.transparentae2.mixin.client;

import appeng.client.render.cablebus.CableBusRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.transparentae2.cable.CableRenderStateExtension;

@Mixin(CableBusRenderState.class)
abstract class CableBusRenderStateMixin implements CableRenderStateExtension {
    @Unique
    private boolean transparentae2$cableTreated;

    @Override
    public boolean transparentae2$isCableTreated() {
        return transparentae2$cableTreated;
    }

    @Override
    public void transparentae2$setCableTreated(boolean treated) {
        transparentae2$cableTreated = treated;
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true)
    private void transparentae2$includeTreatmentInEquality(
            Object other,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()
                && other instanceof CableRenderStateExtension extension
                && extension.transparentae2$isCableTreated() != transparentae2$cableTreated) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true)
    private void transparentae2$includeTreatmentInHash(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(31 * cir.getReturnValue() + Boolean.hashCode(transparentae2$cableTreated));
    }
}
