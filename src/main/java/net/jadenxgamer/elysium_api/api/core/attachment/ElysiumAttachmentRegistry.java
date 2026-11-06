package net.jadenxgamer.elysium_api.api.core.attachment;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A per-mod collector of attachment types, populated through the configurator
 * lambda in {@link ElysiumAttachments#register(String, java.util.function.Consumer)}.
 * <p>
 * Every attachment type registered here is checked against the mod id supplied
 * to the facade, and duplicate ids are rejected. Once the configurator returns,
 * the collected set is handed off to the active backend for actual installation.
 */
public final class ElysiumAttachmentRegistry {

    private final String modId;
    private final Map<ResourceLocation, ElysiumAttachmentType<?>> types = new LinkedHashMap<>();

    @ApiStatus.Internal
    public ElysiumAttachmentRegistry(String modId) {
        this.modId = modId;
    }

    /**
     * Registers an attachment type with this registry.
     * <p>
     * The type's id must be namespaced under the mod id that was supplied to
     * {@link ElysiumAttachments#register(String, java.util.function.Consumer)};
     * registering an id from another namespace is rejected.
     *
     * @param type the attachment type to register
     * @param <T> the type of value stored by the attachment
     * @throws IllegalArgumentException if the id's namespace does not match the mod id
     * @throws IllegalStateException if the id has already been registered
     */
    public <T> void register(ElysiumAttachmentType<T> type) {
        ResourceLocation id = type.id();
        if (!id.getNamespace().equals(this.modId)) throw new IllegalArgumentException("Attachment id '" + id + "' does not match modId '" + this.modId + "'.");
        ElysiumAttachmentType<?> previous = this.types.putIfAbsent(id, type);
        if (previous != null) {
            throw new IllegalStateException("Duplicate attachment registration for id '" + id + "'.");
        }
    }

    @ApiStatus.Internal
    public String modId() {
        return this.modId;
    }

    @ApiStatus.Internal
    public Collection<ElysiumAttachmentType<?>> types() {
        return this.types.values();
    }
}