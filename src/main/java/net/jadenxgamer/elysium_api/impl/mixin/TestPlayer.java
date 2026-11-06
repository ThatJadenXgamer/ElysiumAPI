package net.jadenxgamer.elysium_api.impl.mixin;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.jadenxgamer.elysium_api.mod.registry.ElysiumData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// asphodel::mixin -> loader=*, version=* -> env=common, file=elysium_api.mixins.json
@Mixin(Player.class)
public abstract class TestPlayer {

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void tick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.getBlockStateOn().is(Blocks.AMETHYST_BLOCK)) {
            ElysiumAttachments.setData(player, ElysiumData.MY_DATA, ElysiumAPI.idPath("minecraft", "amethyst_block"));
        } else if (player.getBlockStateOn().is(Blocks.STONE)) {
            ElysiumAttachments.setData(player, ElysiumData.MY_DATA, ElysiumAPI.idPath("minecraft", "stone"));
        }
    }
}