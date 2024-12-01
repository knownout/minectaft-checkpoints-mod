package foundation.kurai.mc.mods.renderers;
import com.mojang.blaze3d.platform.NativeImage;
import foundation.kurai.mc.mods.CheckpointModel;
import foundation.kurai.mc.mods.CheckpointsMod;
import foundation.kurai.mc.mods.util.MetadataUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class CheckpointEntityRenderer {
    private static final ResourceLocation ARROW_ICON_LIT = new ResourceLocation(CheckpointsMod.MODID, "textures/gui/load_highlighted.png"); // Use Minecraft's built-in arrow texture
    private static final ResourceLocation ARROW_ICON = new ResourceLocation(CheckpointsMod.MODID, "textures/gui/load.png"); // Use Minecraft's built-in arrow texture

    private int screenshotWidth = 0;
    private int screenshotHeight = 0;

    public final ResourceLocation screenshotTexture;
    public final CheckpointModel model;

    @FunctionalInterface
    private interface ImageDimensionsConsumer {
        void accept(int a, int b);
    }

    public CheckpointEntityRenderer(CheckpointModel model) {
        this.model = model;
        this.screenshotTexture = loadScreenshot(model.screenshotPath.toFile(), (width, height) -> {
            this.screenshotWidth = width;
            this.screenshotHeight = height;
        });
    }

    private static ResourceLocation loadScreenshot(File screenshotFile, ImageDimensionsConsumer callback) {
        try {
            NativeImage image = NativeImage.read(screenshotFile.toURI().toURL().openStream());

            int screenshotWidth = image.getWidth();
            int screenshotHeight = image.getHeight();

            callback.accept(screenshotWidth, screenshotHeight);
            DynamicTexture texture = new DynamicTexture(image);
            return Minecraft.getInstance().getTextureManager().register("checkpoint_screenshot_" + screenshotFile.getName(), texture);
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Fallback if the image cannot be loaded
        }
    }

    public void render(
            @NotNull GuiGraphics graphics,
            int y,
            int x,
            int rowHeight,
            int mouseX,
            boolean isHovered
    ) {
        int targetWidth = 60;
        int targetHeight = 40;

        // Render the image if available
        if (screenshotTexture != null) {
            // Get the original dimensions of the texture
            int imageWidth = this.screenshotWidth; // Replace with your actual image width
            int imageHeight = this.screenshotHeight; // Replace with your actual image height

            float widthScale = (float) targetWidth / imageWidth;
            float heightScale = (float) targetHeight / imageHeight;

            float scale = Math.max(widthScale, heightScale);

            int scaledWidth = Math.round(imageWidth * scale);
            int scaledHeight = Math.round(imageHeight * scale);

            int offsetX = targetWidth > scaledWidth ? 0 : Math.round((scaledWidth - targetWidth) / 2.0f);
            int offsetY = targetHeight > scaledHeight ? 0 : Math.round((scaledHeight - targetHeight) / 2.0f);

            // Render the scaled image
            graphics.blit(screenshotTexture, x, y, offsetX, offsetY, targetWidth, targetHeight, scaledWidth, scaledHeight);
        } else {
            graphics.drawString(Minecraft.getInstance().font, "No Image", x, y + targetHeight / 2, 0xFF0000, false);
        }

        // Render the checkpoint details with padding
        int textX = x + targetWidth + 10;
        graphics.drawString(Minecraft.getInstance().font, this.model.name, textX, y + 4, 0xFFFFFF, false);
        graphics.drawString(Minecraft.getInstance().font, this.model.fileName + ", " + MetadataUtil.getReadableDatetimeString(this.model.createdAt), textX, y + 18, 0x888888, false);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("screen.checkpoints.version_label").getString() + ": " + MetadataUtil.getModVersion(), textX, y + 28, 0x888888, false);

        if (isHovered) {
            int arrowSize = 24;
            int arrowX = x + targetWidth / 2 - arrowSize / 2; // Position arrow at the end of the row
            int arrowY = y + (rowHeight - arrowSize) / 2; // Center arrow vertically in the row
            graphics.fill(x, y, x + targetWidth, y + targetHeight, 0x55FFFFFF);

            ResourceLocation icon = mouseX <= x + targetWidth ? ARROW_ICON_LIT : ARROW_ICON;
            graphics.blit(icon, arrowX, arrowY, 0, 0, arrowSize, arrowSize, arrowSize, arrowSize);
        }
    }
}
