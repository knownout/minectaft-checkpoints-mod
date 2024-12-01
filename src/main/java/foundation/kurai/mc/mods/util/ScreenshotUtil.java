package foundation.kurai.mc.mods.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.util.function.Consumer;

public class ScreenshotUtil {
    public static void takeGameplayScreenshot(Consumer<NativeImage> callback) {
        Minecraft minecraft = Minecraft.getInstance();

        boolean originalGuiVisibility = minecraft.options.hideGui;

        minecraft.options.hideGui = true;

        RenderSystem.recordRenderCall(() -> {
            callback.accept(Screenshot.takeScreenshot(minecraft.getMainRenderTarget()));

            minecraft.execute(() -> minecraft.options.hideGui = originalGuiVisibility);
        });
    }
}
