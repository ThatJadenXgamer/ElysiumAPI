package net.jadenxgamer.elysium_api.impl.core.item;

import net.jadenxgamer.elysium_api.api.client.screen_flash.ScreenFlash;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PanoramaScreenshotItem extends Item {
    public PanoramaScreenshotItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            Minecraft client = Minecraft.getInstance();
            var originalFov = client.options.fov().get();
            client.options.fov().set(110);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"));
            File screenshotsDir = new File(client.gameDirectory, "screenshots");
            File panoramaDir = new File(screenshotsDir, "panorama_" + timestamp);
            panoramaDir.mkdirs();

            client.grabPanoramixScreenshot(panoramaDir, 1024, 1024);

            File innerScreenshots = new File(panoramaDir, "screenshots");
            if (innerScreenshots.exists() && innerScreenshots.isDirectory()) {
                File[] files = innerScreenshots.listFiles();
                if (files != null) {
                    for (File file : files) {
                        File dest = new File(panoramaDir, file.getName());
                        file.renameTo(dest);
                    }
                }
                innerScreenshots.delete();
            }

            ScreenFlash.triggerScreenFlash(2, 4, 2, 0xAAFFFFFF, false, true);
            Component component = Component.literal(panoramaDir.getName())
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, panoramaDir.getAbsolutePath())));
            player.sendSystemMessage(Component.translatable("screenshot.success", component));
            level.playLocalSound(player, SoundEvents.VAULT_EJECT_ITEM, SoundSource.PLAYERS, 1.0f, 1.5f);

            client.options.fov().set(originalFov);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}