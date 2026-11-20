package net.jadenxgamer.elysium_api.impl.mixin;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.jadenxgamer.elysium_api.api.core.extensions.IElysiumBlock;
import net.jadenxgamer.elysium_api.mod.registry.ElysiumData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

// asphodel::mixin -> version=1.21.1 -> env=common, file=elysium_api.mixins.json
@Mixin(BellBlock.class)
public abstract class TestBE21 implements IElysiumBlock {

    /**
     * @author ThatMaidenJaden
     * @reason just a debugging test, this should not be present on production. if it slipped through then that is just an honest mistake on my part.
     */
    @Overwrite
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        var bell = level.getBlockEntity(pos);
        var stack = player.getMainHandItem();
        boolean success = false;
        if (stack.getItem() instanceof ShovelItem) {
            if (stack.is(Items.DIAMOND_SHOVEL)) {
                if (level.isClientSide) return InteractionResult.PASS;
                ElysiumAttachments.setData(bell, ElysiumData.MY_DATA, ElysiumAPI.idPath("minecraft", "diamond"));
                success = true;
            } else if (stack.is(Items.GOLDEN_SHOVEL)) {
                if (level.isClientSide) return InteractionResult.PASS;
                ElysiumAttachments.setData(bell, ElysiumData.MY_DATA, ElysiumAPI.idPath("minecraft", "gold"));
                success = true;
            } else if (stack.is(Items.IRON_SHOVEL)) {
                if (level.isClientSide) {
                    if (ElysiumAttachments.hasData(bell, ElysiumData.MY_DATA)) {
                        String text = "Data Attachment is: " + ElysiumAttachments.getData(bell, ElysiumData.MY_DATA);
                        player.displayClientMessage(Component.literal(text), true);
                    } else player.displayClientMessage(Component.literal("No Data Attachment"), true);
                }
                success = true;
            }
        }
        return success ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}