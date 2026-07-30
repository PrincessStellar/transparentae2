package rearth.transparentae2.cable;

import appeng.parts.networking.DenseCablePart;
import appeng.parts.networking.SmartCablePart;

public final class CableTreatmentSupport {
    private CableTreatmentSupport() {
    }

    public static boolean isTreatable(Object part) {
        return part instanceof SmartCablePart || part instanceof DenseCablePart;
    }
}
