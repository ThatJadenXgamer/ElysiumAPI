package net.jadenxgamer.elysium_api.mod.registry;

import net.jadenxgamer.elysium_api.ElysiumAPI;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachments;
import net.minecraft.resources.ResourceLocation;

public class ElysiumData {

    public static final ElysiumAttachmentType<ResourceLocation> MY_DATA =
            ElysiumAttachmentType.builder(ElysiumAPI.elysiumPath("my_data"), () -> ElysiumAPI.idPath("minecraft", "stone"))
                    .serialize(ResourceLocation.CODEC)
                    .copyOnDeath()
                    .build();

    public static void init() {
        ElysiumAttachments.register(ElysiumAPI.MOD_ID, registry -> {
            registry.register(MY_DATA);
        });
    }
}