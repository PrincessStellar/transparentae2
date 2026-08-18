package rearth.transparentae2.mixin;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import appeng.parts.AEBasePart;
import appeng.util.SettingsFrom;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.transparentae2.cable.CableTreatmentState;
import rearth.transparentae2.cable.CableTreatmentSupport;
import rearth.transparentae2.init.ModDataComponents;

@Mixin(AEBasePart.class)
abstract class AEBasePartMixin implements CableTreatmentState {
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

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void transparentae2$writeCableTreatment(
            CompoundTag output,
            HolderLookup.Provider registries,
            CallbackInfo ci) {
        if (transparentae2$isTreatableCable() && transparentae2$cableTreated) {
            output.putBoolean("transparentae2CableTreated", true);
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void transparentae2$readCableTreatment(
            CompoundTag input,
            HolderLookup.Provider registries,
            CallbackInfo ci) {
        if (transparentae2$isTreatableCable()) {
            transparentae2$cableTreated = input.getBoolean("transparentae2CableTreated");
        }
    }

    @Inject(method = "writeToStream", at = @At("TAIL"))
    private void transparentae2$writeCableTreatmentToStream(RegistryFriendlyByteBuf data, CallbackInfo ci) {
        if (transparentae2$isTreatableCable()) {
            data.writeBoolean(transparentae2$cableTreated);
        }
    }

    @Inject(method = "readFromStream", at = @At("RETURN"), cancellable = true)
    private void transparentae2$readCableTreatmentFromStream(
            RegistryFriendlyByteBuf data,
            CallbackInfoReturnable<Boolean> cir) {
        if (transparentae2$isTreatableCable()) {
            var treated = data.readBoolean();
            var changed = treated != transparentae2$cableTreated;
            transparentae2$cableTreated = treated;
            cir.setReturnValue(cir.getReturnValue() || changed);
        }
    }

    @Inject(method = "exportSettings", at = @At("TAIL"))
    private void transparentae2$exportCableTreatment(
            SettingsFrom mode,
            DataComponentMap.Builder builder,
            CallbackInfo ci) {
        if (transparentae2$isTreatableCable()
                && mode == SettingsFrom.DISMANTLE_ITEM
                && transparentae2$cableTreated) {
            builder.set(ModDataComponents.CABLE_TREATED.get(), true);
        }
    }

    @Inject(method = "importSettings", at = @At("TAIL"))
    private void transparentae2$importCableTreatment(
            SettingsFrom mode,
            DataComponentMap input,
            @Nullable Player player,
            CallbackInfo ci) {
        if (transparentae2$isTreatableCable() && mode == SettingsFrom.DISMANTLE_ITEM) {
            transparentae2$cableTreated = input.getOrDefault(ModDataComponents.CABLE_TREATED.get(), false);
            var host = ((AEBasePart) (Object) this).getHost();
            if (host != null) {
                host.markForUpdate();
                host.markForSave();
            }
        }
    }

    @Unique
    private boolean transparentae2$isTreatableCable() {
        return CableTreatmentSupport.isTreatable(this);
    }
}
