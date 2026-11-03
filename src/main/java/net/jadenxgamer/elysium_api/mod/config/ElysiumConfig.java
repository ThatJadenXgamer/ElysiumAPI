package net.jadenxgamer.elysium_api.mod.config;

import net.jadenxgamer.elysium_api.api.config.ElysiumConfigEntry;
import net.jadenxgamer.elysium_api.api.config.ConfigLoadType;

public class ElysiumConfig {

    public static final ElysiumConfigEntry<Boolean> ENABLED =
            ElysiumConfigEntry.define("enabled", true)
            .category("general")
            .comment("Enables the feature");

    public static final ElysiumConfigEntry<Integer> AMOUNT =
            ElysiumConfigEntry.defineInRange("amount", 10, 1, 100)
            .category("general")
            .comment("How much of the thing to do");

    public static final ElysiumConfigEntry<MyEnum> MODE =
            ElysiumConfigEntry.define("mode", MyEnum.SURVIVAL, MyEnum.class)
            .category("general.other")
            .comment("Which mode to run in");

    public static final ElysiumConfigEntry<Boolean> SPONGE_RIGHT_CLICK_TEST =
            ElysiumConfigEntry.define("sponge_right_click_test", true)
            .category("test").loadType(ConfigLoadType.SERVER)
            .comment("If true, right-clicking a sponge logs the current value from both sides");
}