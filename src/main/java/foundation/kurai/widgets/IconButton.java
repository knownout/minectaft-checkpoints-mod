package foundation.kurai.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class IconButton extends Button {
    private final ResourceLocation icon; // The icon to render
    private final int iconWidth;
    private final int iconHeight;

    public IconButton(int x, int y, int width, int height, ResourceLocation icon, int iconWidth, int iconHeight, OnPress onPress) {
        super(Button.builder(
                Component.empty(),
                onPress
        ).bounds(x, y, width, height));
        this.icon = icon;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Calculate the center position to draw the icon
        int iconX = this.getX() + (this.width - iconWidth) / 2;
        int iconY = this.getY() + (this.height - iconHeight) / 2;

        // Bind the texture and render the icon
        graphics.blit(icon, iconX, iconY, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);
    }
}