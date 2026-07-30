package rearth.transparentae2.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import rearth.transparentae2.TransferLogger;

@Mixin(PatternProviderLogic.class)
abstract class PatternProviderLogicMixin {
    @Shadow
    @Final
    private PatternProviderLogicHost host;

    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void transparentae2$logCraftingDispatch(
            IPatternDetails pattern,
            KeyCounter[] inputs,
            CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) {
            return;
        }

        for (var slot : inputs) {
            for (var entry : slot) {
                TransferLogger.logCraftingDispatch(entry.getKey(), entry.getLongValue(), host);
            }
        }
    }
}
