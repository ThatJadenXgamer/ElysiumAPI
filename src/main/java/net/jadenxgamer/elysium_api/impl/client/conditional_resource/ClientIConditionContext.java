package net.jadenxgamer.elysium_api.impl.client.conditional_resource;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ClientIConditionContext implements ICondition.IContext {
    public static final ClientIConditionContext INSTANCE = new ClientIConditionContext();

    private ClientIConditionContext() {}

    @Override
    public <T> Map<ResourceLocation, Collection<Holder<T>>> getAllTags(ResourceKey<? extends Registry<T>> registry) {
        ElysiumAPI.LOGGER.debug("Tag access attempted in client condition context - tags are unsupported for conditionalProperties-loaded assets");
        return Collections.emptyMap();
    }
}