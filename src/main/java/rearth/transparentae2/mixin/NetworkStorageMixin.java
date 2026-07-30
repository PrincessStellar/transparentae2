package rearth.transparentae2.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.storage.NetworkStorage;
import rearth.transparentae2.TransferLogger;

@Mixin(NetworkStorage.class)
abstract class NetworkStorageMixin {
    @Inject(method = "insert", at = @At("RETURN"))
    private void transparentae2$logInsert(
            AEKey what,
            long amount,
            Actionable mode,
            IActionSource source,
            CallbackInfoReturnable<Long> callback) {
        if (mode == Actionable.MODULATE) {
            TransferLogger.logNetworkInsert(what, callback.getReturnValue(), source);
        }
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void transparentae2$logExtract(
            AEKey what,
            long amount,
            Actionable mode,
            IActionSource source,
            CallbackInfoReturnable<Long> callback) {
        if (mode == Actionable.MODULATE) {
            TransferLogger.logNetworkExtract(what, callback.getReturnValue(), source);
        }
    }
}
