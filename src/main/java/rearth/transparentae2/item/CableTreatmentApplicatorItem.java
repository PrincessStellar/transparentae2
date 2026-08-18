package rearth.transparentae2.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.parts.networking.CablePart;
import rearth.transparentae2.cable.CableTreatmentState;
import rearth.transparentae2.cable.CableTreatmentSupport;

public final class CableTreatmentApplicatorItem extends Item {
    public CableTreatmentApplicatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player == null || !level.mayInteract(player, pos)
                || !(level.getBlockEntity(pos) instanceof CableBusBlockEntity cableBus)) {
            return InteractionResult.PASS;
        }

        var selected = cableBus.getCableBus().selectPartWorld(context.getClickLocation());
        if (!(selected.part instanceof CablePart cable)
                || !CableTreatmentSupport.isTreatable(cable)
                || !(cable instanceof CableTreatmentState treatment)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            var treated = !treatment.transparentae2$isCableTreated();
            treatment.transparentae2$setCableTreated(treated);
            var host = cable.getHost();
            host.markForUpdate();
            host.markForSave();
            player.displayClientMessage(Component.translatable(
                    treated
                            ? "message.transparentae2.cable_treatment_applied"
                            : "message.transparentae2.cable_treatment_removed"), true);
        }

        return InteractionResult.SUCCESS;
    }
}
