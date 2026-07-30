package rearth.transparentae2.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;
import rearth.transparentae2.TransparentAE2;
import rearth.transparentae2.network.TransferPathPayload;

public final class ClientTransferPathRenderer {
    private static final int MAX_TRANSFERS = 128;
    private static final long DEBUG_PATH_LIFETIME_MILLIS = 5_000;
    private static final double ITEMS_PER_SECOND = 8.0;
    private static final double LINE_Y_OFFSET = 0.5;
    private static final ContextKey<List<MovingItemRenderState>> ITEM_RENDER_STATES = new ContextKey<>(
            Identifier.fromNamespaceAndPath(TransparentAE2.MODID, "moving_transfer_items"));
    private static final Deque<MovingTransfer> TRANSFERS = new ArrayDeque<>();

    private ClientTransferPathRenderer() {
    }

    public static void add(TransferPathPayload payload) {
        while (TRANSFERS.size() >= MAX_TRANSFERS) {
            TRANSFERS.removeFirst();
        }
        TRANSFERS.addLast(MovingTransfer.create(payload, Util.getMillis()));
    }

    public static void clear() {
        TRANSFERS.clear();
    }

    public static void extract(ExtractLevelRenderStateEvent event) {
        if (TRANSFERS.isEmpty()) {
            return;
        }

        var now = Util.getMillis();
        TRANSFERS.removeIf(transfer -> transfer.retentionEndsAtMillis <= now);
        var renderStates = new ArrayList<MovingItemRenderState>();
        var modelResolver = Minecraft.getInstance().getItemModelResolver();

        for (var transfer : TRANSFERS) {
            var position = transfer.positionAt(now);
            if (position == null || !position.dimension.equals(event.getLevel().dimension())) {
                continue;
            }

            var itemState = new ItemStackRenderState();
            modelResolver.updateForTopItem(
                    itemState,
                    transfer.payload.stack(),
                    ItemDisplayContext.GROUND,
                    event.getLevel(),
                    null,
                    transfer.seed);
            var light = LevelRenderer.getLightCoords(event.getLevel(), BlockPos.containing(position.position));
            var rotation = (now - transfer.startedAtMillis) * 0.002F;
            renderStates.add(new MovingItemRenderState(position.position, itemState, light, rotation));
        }

        if (!renderStates.isEmpty()) {
            event.getRenderState().setRenderData(ITEM_RENDER_STATES, List.copyOf(renderStates));
        }
    }

    public static void submitItems(SubmitCustomGeometryEvent event) {
        List<MovingItemRenderState> renderStates = event.getLevelRenderState().getRenderData(ITEM_RENDER_STATES);
        if (renderStates == null) {
            return;
        }

        var poseStack = event.getPoseStack();
        var camera = event.getLevelRenderState().cameraRenderState.pos;
        for (var renderState : renderStates) {
            var position = renderState.position.subtract(camera);
            poseStack.pushPose();
            poseStack.translate(position.x, position.y, position.z);
            poseStack.mulPose(Axis.YP.rotation(renderState.rotation));
            renderState.item.submit(
                    poseStack,
                    event.getSubmitNodeCollector(),
                    renderState.light,
                    OverlayTexture.NO_OVERLAY,
                    0);
            poseStack.popPose();
        }
    }

    public static void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (!ClientConfig.DEBUG_RENDER_TRANSFER_PATHS.getAsBoolean() || TRANSFERS.isEmpty()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            return;
        }

        var now = Util.getMillis();
        var poseStack = event.getPoseStack();
        var camera = event.getLevelRenderState().cameraRenderState.pos;
        var consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderTypes.linesTranslucent());

        for (var transfer : TRANSFERS) {
            if (transfer.debugEndsAtMillis > now) {
                var color = color(transfer.payload.kind());
                for (var leg : transfer.payload.legs()) {
                    renderLeg(poseStack, consumer, camera, level.dimension(), leg, color);
                }
            }
        }
    }

    private static void renderLeg(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 camera,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            List<GlobalPos> points,
            int color) {
        GlobalPos previous = null;
        for (var point : points) {
            if (!point.dimension().equals(dimension)) {
                previous = null;
                continue;
            }

            if (previous != null && previous.dimension().equals(point.dimension())) {
                var from = elevatedCenter(previous).subtract(camera);
                var to = elevatedCenter(point).subtract(camera);
                renderLine(poseStack, consumer, from, to, color);
            }
            previous = point;
        }
    }

    private static Vec3 elevatedCenter(GlobalPos point) {
        return Vec3.atCenterOf(point.pos()).add(0, LINE_Y_OFFSET, 0);
    }

    private static void renderLine(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 from,
            Vec3 to,
            int color) {
        var direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }

        var normal = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z).normalize();
        var pose = poseStack.last();
        consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(4.0F);
        consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(color)
                .setNormal(pose, normal)
                .setLineWidth(4.0F);
    }

    private static int color(TransferPathPayload.PathKind kind) {
        return switch (kind) {
            case IMPORT -> 0xFF55FF55;
            case INSERT -> 0xFFFFFF55;
            case EXPORT -> 0xFFFFAA33;
            case EXTRACT -> 0xFF55FFFF;
            case CRAFTING -> 0xFFFF55FF;
        };
    }

    private record MovingItemRenderState(Vec3 position, ItemStackRenderState item, int light, float rotation) {
    }

    private record PositionedItem(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            Vec3 position) {
    }

    private static final class MovingTransfer {
        private final TransferPathPayload payload;
        private final long startedAtMillis;
        private final long travelEndsAtMillis;
        private final long debugEndsAtMillis;
        private final long retentionEndsAtMillis;
        private final double routeLength;
        private final int seed;

        private MovingTransfer(
                TransferPathPayload payload,
                long startedAtMillis,
                long travelEndsAtMillis,
                long debugEndsAtMillis,
                double routeLength) {
            this.payload = payload;
            this.startedAtMillis = startedAtMillis;
            this.travelEndsAtMillis = travelEndsAtMillis;
            this.debugEndsAtMillis = debugEndsAtMillis;
            this.retentionEndsAtMillis = Math.max(travelEndsAtMillis, debugEndsAtMillis);
            this.routeLength = routeLength;
            this.seed = payload.stack().hashCode();
        }

        static MovingTransfer create(TransferPathPayload payload, long now) {
            var routeLength = routeLength(payload.legs());
            var travelMillis = Math.max(250L, Math.round(routeLength / ITEMS_PER_SECOND * 1_000.0));
            return new MovingTransfer(
                    payload,
                    now,
                    now + travelMillis,
                    now + DEBUG_PATH_LIFETIME_MILLIS,
                    routeLength);
        }

        PositionedItem positionAt(long now) {
            if (now >= travelEndsAtMillis) {
                return null;
            }

            if (routeLength <= 1.0E-6) {
                return firstPosition(payload.legs());
            }

            var elapsed = Math.max(0L, now - startedAtMillis);
            var distance = Math.min(routeLength, elapsed / 1_000.0 * ITEMS_PER_SECOND);
            for (var leg : payload.legs()) {
                for (int pointIndex = 1; pointIndex < leg.size(); pointIndex++) {
                    var from = leg.get(pointIndex - 1);
                    var to = leg.get(pointIndex);
                    if (!from.dimension().equals(to.dimension())) {
                        continue;
                    }

                    var fromPosition = elevatedCenter(from);
                    var toPosition = elevatedCenter(to);
                    var edgeLength = fromPosition.distanceTo(toPosition);
                    if (distance <= edgeLength) {
                        var progress = edgeLength <= 1.0E-6 ? 1.0 : distance / edgeLength;
                        return new PositionedItem(from.dimension(), fromPosition.lerp(toPosition, progress));
                    }
                    distance -= edgeLength;
                }
            }
            return lastPosition(payload.legs());
        }

        private static double routeLength(List<List<GlobalPos>> legs) {
            double result = 0;
            for (var leg : legs) {
                for (int pointIndex = 1; pointIndex < leg.size(); pointIndex++) {
                    var from = leg.get(pointIndex - 1);
                    var to = leg.get(pointIndex);
                    if (from.dimension().equals(to.dimension())) {
                        result += elevatedCenter(from).distanceTo(elevatedCenter(to));
                    }
                }
            }
            return result;
        }

        private static PositionedItem firstPosition(List<List<GlobalPos>> legs) {
            for (var leg : legs) {
                if (!leg.isEmpty()) {
                    var point = leg.getFirst();
                    return new PositionedItem(point.dimension(), elevatedCenter(point));
                }
            }
            return null;
        }

        private static PositionedItem lastPosition(List<List<GlobalPos>> legs) {
            for (int legIndex = legs.size() - 1; legIndex >= 0; legIndex--) {
                var leg = legs.get(legIndex);
                if (!leg.isEmpty()) {
                    var point = leg.getLast();
                    return new PositionedItem(point.dimension(), elevatedCenter(point));
                }
            }
            return null;
        }
    }
}
