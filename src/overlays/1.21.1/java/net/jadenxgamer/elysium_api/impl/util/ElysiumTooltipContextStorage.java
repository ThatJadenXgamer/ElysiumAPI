package net.jadenxgamer.elysium_api.impl.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ElysiumTooltipContextStorage {

    private static final Map<Item.TooltipContext, Level> CAPTURED_LEVELS = Collections.synchronizedMap(new WeakHashMap<>());

    private ElysiumTooltipContextStorage() {
    }

    public static void capture(Item.TooltipContext context, Level level) {
        if (CAPTURED_LEVELS.size() > 256) CAPTURED_LEVELS.clear();
        CAPTURED_LEVELS.put(context, level);
    }

    public static @Nullable Level get(Item.TooltipContext context) {
        return CAPTURED_LEVELS.get(context);
    }
}