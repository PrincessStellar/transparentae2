package rearth.transparentae2.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import rearth.transparentae2.TransferLogger;
import rearth.transparentae2.TransparentAE2;

@Mixin(CraftingCpuLogic.class)
abstract class CraftingCpuLogicMixin {
    @Shadow
    @Final
    CraftingCPUCluster cluster;

    @Redirect(
            method = "executeCrafting",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern"
                            + "(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"))
    private boolean transparentae2$logCraftingDispatch(
            ICraftingProvider provider,
            IPatternDetails pattern,
            KeyCounter[] inputs) {
        var transferred = new ArrayList<GenericStack>();
        for (var slot : inputs) {
            for (var entry : slot) {
                transferred.add(new GenericStack(entry.getKey(), entry.getLongValue()));
            }
        }

        var accepted = provider.pushPattern(pattern, inputs);
        if (accepted) {
            try {
                var cpuNode = cluster.getNode();
                var providerNode = findProviderNode(provider);
                for (var stack : transferred) {
                    TransferLogger.logCraftingDispatch(stack.what(), stack.amount(), cpuNode, providerNode);
                }
            } catch (RuntimeException e) {
                TransparentAE2.LOGGER.warn("Failed to observe an AE2 crafting dispatch", e);
            }
        }
        return accepted;
    }

    private IGridNode findProviderNode(ICraftingProvider provider) {
        var grid = cluster.getGrid();
        if (grid == null) {
            return null;
        }

        for (var node : grid.getNodes()) {
            if (node.getService(ICraftingProvider.class) == provider) {
                return node;
            }
        }
        return null;
    }
}
