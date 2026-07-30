package rearth.transparentae2.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import rearth.transparentae2.TransparentAE2;

public record TransferPathPayload(
        PathKind kind,
        ItemStack stack,
        long amount,
        List<List<GlobalPos>> legs) implements CustomPacketPayload {
    private static final int MAX_LEGS = 64;
    private static final int MAX_POINTS_PER_LEG = 512;

    public static final Type<TransferPathPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TransparentAE2.MODID, "transfer_path"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferPathPayload> STREAM_CODEC = StreamCodec.ofMember(
            TransferPathPayload::encode,
            TransferPathPayload::decode);

    public TransferPathPayload {
        stack = stack.copy();
        legs = legs.stream().map(List::copyOf).toList();
        if (legs.size() > MAX_LEGS || legs.stream().anyMatch(leg -> leg.size() > MAX_POINTS_PER_LEG)) {
            throw new IllegalArgumentException("Transfer path exceeds payload limits");
        }
    }

    @Override
    public Type<TransferPathPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(kind.ordinal());
        ItemStack.STREAM_CODEC.encode(buffer, stack);
        buffer.writeVarLong(amount);
        buffer.writeVarInt(legs.size());
        for (var leg : legs) {
            buffer.writeVarInt(leg.size());
            for (var point : leg) {
                GlobalPos.STREAM_CODEC.encode(buffer, point);
            }
        }
    }

    private static TransferPathPayload decode(RegistryFriendlyByteBuf buffer) {
        var kindIndex = buffer.readVarInt();
        if (kindIndex < 0 || kindIndex >= PathKind.values().length) {
            throw new IllegalArgumentException("Unknown transfer path kind " + kindIndex);
        }

        var stack = ItemStack.STREAM_CODEC.decode(buffer);
        var amount = buffer.readVarLong();
        var legCount = readBoundedSize(buffer, MAX_LEGS, "legs");
        var legs = new ArrayList<List<GlobalPos>>(legCount);
        for (int legIndex = 0; legIndex < legCount; legIndex++) {
            var pointCount = readBoundedSize(buffer, MAX_POINTS_PER_LEG, "points");
            var points = new ArrayList<GlobalPos>(pointCount);
            for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                points.add(GlobalPos.STREAM_CODEC.decode(buffer));
            }
            legs.add(points);
        }
        return new TransferPathPayload(PathKind.values()[kindIndex], stack, amount, legs);
    }

    private static int readBoundedSize(RegistryFriendlyByteBuf buffer, int maximum, String name) {
        var size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException("Invalid number of transfer path " + name + ": " + size);
        }
        return size;
    }

    public enum PathKind {
        IMPORT,
        INSERT,
        EXPORT,
        EXTRACT,
        CRAFTING
    }
}
