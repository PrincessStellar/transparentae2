package rearth.transparentae2.client;

import java.util.ArrayDeque;
import java.util.Deque;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import rearth.transparentae2.network.TransferPathPayload;

public final class ClientTransferPathRenderer {
    private static final int MAX_PATHS = 32;
    private static final long PATH_LIFETIME_MILLIS = 5_000;
    private static final double LINE_Y_OFFSET = 0.5;
    private static final Deque<RenderedPath> PATHS = new ArrayDeque<>();

    private ClientTransferPathRenderer() {
    }

    public static void add(TransferPathPayload payload) {
        while (PATHS.size() >= MAX_PATHS) {
            PATHS.removeFirst();
        }
        PATHS.addLast(new RenderedPath(payload, Util.getMillis() + PATH_LIFETIME_MILLIS));
    }

    public static void clear() {
        PATHS.clear();
    }

    public static void render(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || PATHS.isEmpty()) {
            return;
        }

        var now = Util.getMillis();
        PATHS.removeIf(path -> path.expiresAtMillis <= now);
        if (PATHS.isEmpty()) {
            return;
        }

        var poseStack = event.getPoseStack();
        var camera = event.getLevelRenderState().cameraRenderState.pos;
        var consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderTypes.linesTranslucent());

        for (var renderedPath : PATHS) {
            var color = color(renderedPath.payload.kind());
            for (var leg : renderedPath.payload.legs()) {
                renderLeg(poseStack, consumer, camera, level.dimension(), leg, color);
            }
        }
    }

    private static void renderLeg(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 camera,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            java.util.List<GlobalPos> points,
            int color) {
        GlobalPos previous = null;
        for (var point : points) {
            if (!point.dimension().equals(dimension)) {
                previous = null;
                continue;
            }

            var pos = point.pos();
            ShapeRenderer.renderShape(
                    poseStack,
                    consumer,
                    Shapes.block(),
                    pos.getX() - camera.x,
                    pos.getY() - camera.y,
                    pos.getZ() - camera.z,
                    color,
                    2.5F);

            if (previous != null && previous.dimension().equals(point.dimension())) {
                var from = Vec3.atCenterOf(previous.pos()).add(0, LINE_Y_OFFSET, 0).subtract(camera);
                var to = Vec3.atCenterOf(pos).add(0, LINE_Y_OFFSET, 0).subtract(camera);
                renderLine(poseStack, consumer, from, to, color);
            }
            previous = point;
        }
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

    private record RenderedPath(TransferPathPayload payload, long expiresAtMillis) {
    }
}
