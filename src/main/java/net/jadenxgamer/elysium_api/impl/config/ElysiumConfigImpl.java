package net.jadenxgamer.elysium_api.impl.config;
\
import net.minecraftforge.common.ForgeConfigSpec;

import static net.jadenxgamer.elysium_api.impl.config.ElysiumConfig.*;

public class ElysiumConfigImpl {

    public static ForgeConfigSpec COMMON;

    static {
        ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();

        COMMON.comment("Building Tools").push("buildingTools");
        BuildingTools.init(COMMON);
        COMMON.pop();

        ElysiumConfigImpl.COMMON = COMMON.build();
    }


    public static class BuildingTools {
        public static void init(ForgeConfigSpec.Builder builder) {
            BLOCK_SWAP = builder
                    .comment("Replace blocks without breaking them first")
                    .define("blockSwap", false);
        }
    }
}