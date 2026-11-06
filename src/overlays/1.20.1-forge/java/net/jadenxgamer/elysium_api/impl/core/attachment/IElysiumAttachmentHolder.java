package net.jadenxgamer.elysium_api.impl.core.attachment;

import net.jadenxgamer.elysium_api.api.core.attachment.ElysiumAttachmentType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public interface IElysiumAttachmentHolder {

    @Nullable
    <T> T getData(ElysiumAttachmentType<T> type);

    <T> void setData(ElysiumAttachmentType<T> type, @Nullable T value);

    <T> boolean hasData(ElysiumAttachmentType<T> type);
}