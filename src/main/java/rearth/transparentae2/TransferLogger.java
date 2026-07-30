package rearth.transparentae2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.parts.AEBasePart;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.automation.ImportBusPart;

public final class TransferLogger {
    private static final String NETWORK = "ME network";

    private TransferLogger() {
    }

    public static void logNetworkInsert(AEKey what, long amount, IActionSource source) {
        if (amount > 0 && what instanceof AEItemKey item) {
            var kind = source.machine().orElse(null) instanceof ImportBusPart ? "IMPORT" : "INSERT";
            log(kind, item, amount, sourceEndpoint(source, true), NETWORK);
            logPath(kind, item, amount, sourceNode(source), false);
        }
    }

    public static void logNetworkExtract(AEKey what, long amount, IActionSource source) {
        if (amount > 0 && what instanceof AEItemKey item) {
            var kind = source.machine().orElse(null) instanceof ExportBusPart ? "EXPORT" : "EXTRACT";
            log(kind, item, amount, NETWORK, sourceEndpoint(source, false));
            logPath(kind, item, amount, sourceNode(source), true);
        }
    }

    public static void logCraftingDispatch(AEKey what, long amount, IGridNode cpu, IGridNode provider) {
        if (amount > 0 && what instanceof AEItemKey item) {
            log("CRAFTING", item, amount, "crafting CPU",
                    "crafting provider " + describeNode(provider));
            if (Config.LOG_TRANSFER_PATHS.getAsBoolean()) {
                logPath("CRAFTING", item, amount, TransferPathResolver.resolveCrafting(cpu, provider));
            }
        }
    }

    private static void log(String kind, AEItemKey item, long amount, String from, String to) {
        if (!Config.LOG_ITEM_TRANSFERS.getAsBoolean()) {
            return;
        }

        var stack = item.getReadOnlyStack();
        var itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        var components = stack.getComponentsPatch().isEmpty() ? "" : " components=" + stack.getComponentsPatch();
        TransparentAE2.LOGGER.info("[AE2 TRANSFER/{}] {}x {}{} | {} -> {}",
                kind, amount, itemId, components, from, to);
    }

    private static void logPath(
            String kind,
            AEItemKey item,
            long amount,
            IGridNode endpoint,
            boolean controllerFirst) {
        if (Config.LOG_TRANSFER_PATHS.getAsBoolean()) {
            logPath(kind, item, amount, TransferPathResolver.resolve(endpoint, controllerFirst));
        }
    }

    private static void logPath(String kind, AEItemKey item, long amount, String path) {
        var itemId = BuiltInRegistries.ITEM.getKey(item.getItem());
        TransparentAE2.LOGGER.info("[AE2 PATH/{}] {}x {} | {}", kind, amount, itemId, path);
    }

    private static IGridNode sourceNode(IActionSource source) {
        return source.machine()
                .map(IActionHost::getActionableNode)
                .orElse(null);
    }

    private static String sourceEndpoint(IActionSource source, boolean inserting) {
        var machine = source.machine().orElse(null);
        if (machine != null) {
            return describeMachine(machine, inserting);
        }

        return source.player()
                .map(TransferLogger::describePlayer)
                .orElse("unknown source");
    }

    private static String describeMachine(IActionHost machine, boolean inserting) {
        if (machine instanceof AEBasePart part) {
            var position = part.getBlockEntity().getBlockPos();
            var side = part.getSide();

            if (side != null
                    && ((inserting && part instanceof ImportBusPart)
                            || (!inserting && part instanceof ExportBusPart))) {
                position = position.relative(side);
                return part.getClass().getSimpleName() + " external " + describe(part.getBlockEntity(), position);
            }

            return part.getClass().getSimpleName() + " " + describe(part.getBlockEntity(), position);
        }

        var node = machine.getActionableNode();
        if (node != null) {
            var owner = node.getOwner();
            if (owner instanceof BlockEntity blockEntity) {
                return machine.getClass().getSimpleName() + " " + describe(blockEntity);
            }
            if (owner instanceof AEBasePart part) {
                return part.getClass().getSimpleName() + " " + describe(part.getBlockEntity());
            }
        }

        return machine.getClass().getSimpleName();
    }

    private static String describeNode(IGridNode node) {
        if (node == null) {
            return "(unknown position)";
        }

        var owner = node.getOwner();
        if (owner instanceof BlockEntity blockEntity) {
            return owner.getClass().getSimpleName() + " " + describe(blockEntity);
        }
        if (owner instanceof AEBasePart part) {
            return owner.getClass().getSimpleName() + " " + describe(part.getBlockEntity());
        }
        return owner.getClass().getSimpleName() + " (no world position)";
    }

    private static String describePlayer(Player player) {
        return "player " + player.getGameProfile().name() + " @ "
                + player.level().dimension().identifier() + " " + player.blockPosition().toShortString();
    }

    private static String describe(BlockEntity blockEntity) {
        return describe(blockEntity, blockEntity.getBlockPos());
    }

    private static String describe(BlockEntity blockEntity, BlockPos position) {
        var level = blockEntity.getLevel();
        var dimension = level == null ? "unloaded" : level.dimension().identifier().toString();
        return "@ " + dimension + " " + position.toShortString();
    }
}
