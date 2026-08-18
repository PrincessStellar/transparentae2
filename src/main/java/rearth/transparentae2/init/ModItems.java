package rearth.transparentae2.init;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.api.ids.AECreativeTabIds;
import appeng.api.util.AEColor;
import appeng.items.parts.ColoredPartItem;
import appeng.parts.networking.CoveredDenseCablePart;
import appeng.parts.networking.SmartCablePart;
import appeng.parts.networking.SmartDenseCablePart;
import rearth.transparentae2.TransparentAE2;
import rearth.transparentae2.item.CableTreatmentApplicatorItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TransparentAE2.MODID);

    public static final DeferredItem<CableTreatmentApplicatorItem> CABLE_TREATMENT_APPLICATOR = ITEMS.registerItem(
            "cable_treatment_applicator",
            CableTreatmentApplicatorItem::new,
            new Item.Properties().stacksTo(1));

    public static final DeferredItem<ColoredPartItem<SmartCablePart>> TRANSPARENT_SMART_CABLE = ITEMS.registerItem(
            "transparent_smart_cable",
            properties -> new ColoredPartItem<>(
                    treatedCableProperties(properties),
                    SmartCablePart.class,
                    SmartCablePart::new,
                    AEColor.TRANSPARENT),
            new Item.Properties());

    public static final DeferredItem<ColoredPartItem<SmartDenseCablePart>> TRANSPARENT_SMART_DENSE_CABLE =
            ITEMS.registerItem(
                    "transparent_smart_dense_cable",
                    properties -> new ColoredPartItem<>(
                            treatedCableProperties(properties),
                            SmartDenseCablePart.class,
                            SmartDenseCablePart::new,
                            AEColor.TRANSPARENT),
                    new Item.Properties());

    public static final DeferredItem<ColoredPartItem<CoveredDenseCablePart>> TRANSPARENT_COVERED_DENSE_CABLE =
            ITEMS.registerItem(
                    "transparent_covered_dense_cable",
                    properties -> new ColoredPartItem<>(
                            treatedCableProperties(properties),
                            CoveredDenseCablePart.class,
                            CoveredDenseCablePart::new,
                            AEColor.TRANSPARENT),
                    new Item.Properties());

    private ModItems() {
    }

    private static Item.Properties treatedCableProperties(Item.Properties properties) {
        return properties.component(ModDataComponents.CABLE_TREATED.get(), true);
    }

    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CABLE_TREATMENT_APPLICATOR);
        } else if (event.getTabKey() == AECreativeTabIds.MAIN) {
            event.accept(TRANSPARENT_SMART_CABLE);
            event.accept(TRANSPARENT_SMART_DENSE_CABLE);
            event.accept(TRANSPARENT_COVERED_DENSE_CABLE);
        }
    }
}
