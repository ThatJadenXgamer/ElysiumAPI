package net.jadenxgamer.elysium_api.impl.compat;

import net.minecraft.world.level.levelgen.SurfaceRules;
import terrablender.api.SurfaceRuleManager;

public class ElysiumTerrablenderHelper {

    public static void addOverworldSurfaceRule(SurfaceRules.RuleSource rules, String namespace) {
        SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, namespace, rules);
    }

    public static void addNetherSurfaceRule(SurfaceRules.RuleSource rules, String namespace) {
        SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.NETHER, namespace, rules);
    }
}
