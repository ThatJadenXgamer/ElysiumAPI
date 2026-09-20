package net.jadenxgamer.elysium_api.impl.mixin.misc;

import net.jadenxgamer.elysium_api.impl.util.StructureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JigsawReplacementProcessor.class)
public class JigsawReplacementProcessorMixin {

    @Inject(
            method = "processBlock",
            at = @At("RETURN"),
            cancellable = true
    )
    private void elysium$skipSolidStructureVoid(
            LevelReader serverLevel,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo original,
            StructureTemplate.StructureBlockInfo processed,
            StructurePlaceSettings settings,
            CallbackInfoReturnable<StructureTemplate.StructureBlockInfo> cir
    ) {
        StructureTemplate.StructureBlockInfo result = cir.getReturnValue();
        if (result != null && StructureHelper.IGNORE_BLOCK_SUPPLIERS.contains(result.state().getBlock())) {
            cir.setReturnValue(null);
        }
    }
}