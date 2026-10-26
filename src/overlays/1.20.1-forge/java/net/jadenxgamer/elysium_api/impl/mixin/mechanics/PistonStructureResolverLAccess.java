package net.jadenxgamer.elysium_api.impl.mixin.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PistonStructureResolver.class)
public interface PistonStructureResolverLAccess {

    @Accessor("level")
    Level elysium$getLevel();

    @Invoker("addBlockLine")
    boolean elysium$invokeAddBlockLine(BlockPos originPos, Direction direction);
}