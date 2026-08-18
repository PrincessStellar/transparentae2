package rearth.transparentae2.client;

import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.client.render.cablebus.CableCoreType;

public final class CableRenderContext {
    private static final ThreadLocal<Boolean> TREATMENT_ACTIVE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> CHANNEL_OVERLAY = ThreadLocal.withInitial(() -> false);
    private static volatile Map<TextureAtlasSprite, TextureAtlasSprite> baseCableTextures = Map.of();

    private CableRenderContext() {
    }

    public static void begin(boolean treatmentActive) {
        TREATMENT_ACTIVE.set(treatmentActive);
    }

    public static void end() {
        TREATMENT_ACTIVE.remove();
        CHANNEL_OVERLAY.remove();
    }

    public static boolean isTreatmentActive() {
        return TREATMENT_ACTIVE.get();
    }

    public static boolean shouldSkipGeometry() {
        return TREATMENT_ACTIVE.get() && CHANNEL_OVERLAY.get();
    }

    public static TextureAtlasSprite identifyCableTexture(TextureAtlasSprite texture) {
        if (TREATMENT_ACTIVE.get()) {
            var replacement = baseCableTextures.get(texture);
            CHANNEL_OVERLAY.set(replacement == null);
            return replacement != null ? replacement : texture;
        }
        return texture;
    }

    public static void registerCableTextures(
            EnumMap<CableCoreType, EnumMap<AEColor, TextureAtlasSprite>> coreTextures,
            EnumMap<AECableType, EnumMap<AEColor, TextureAtlasSprite>> connectionTextures) {
        var textures = new IdentityHashMap<TextureAtlasSprite, TextureAtlasSprite>();
        for (var texturesByColor : coreTextures.values()) {
            for (var texture : texturesByColor.values()) {
                textures.put(texture, texture);
            }
        }
        for (var texturesByColor : connectionTextures.values()) {
            for (var texture : texturesByColor.values()) {
                textures.put(texture, texture);
            }
        }

        var denseCovered = connectionTextures.get(AECableType.DENSE_COVERED);
        var denseSmart = connectionTextures.get(AECableType.DENSE_SMART);
        for (var color : AEColor.values()) {
            textures.put(denseCovered.get(color), denseSmart.get(color));
        }
        baseCableTextures = Collections.unmodifiableMap(textures);
    }
}
