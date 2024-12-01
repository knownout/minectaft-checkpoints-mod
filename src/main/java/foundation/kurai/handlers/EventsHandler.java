package foundation.kurai.handlers;

import foundation.kurai.CheckpointModel;
import foundation.kurai.screens.CheckpointsScreen;
import foundation.kurai.util.CheckpointUtil;
import foundation.kurai.widgets.IconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "checkpoints_mod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventsHandler {

    private static final ResourceLocation ICON = new ResourceLocation("checkpoints_mod", "textures/gui/checkpoints_list.png");

    private static final CheckpointsScreen checkpointsScreen = new CheckpointsScreen();

    @SubscribeEvent
    public static void onPauseMenuInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        if (!Minecraft.getInstance().isSingleplayer()) return;

        if (screen instanceof PauseScreen) {
            for (var widget : event.getListenersList()) {
                if (widget instanceof Button button &&
                        button.getMessage().getString().equals(Component.translatable("menu.returnToMenu").getString())) {

                    button.setWidth(button.getWidth() - 24);

                    IconButton iconButton = new IconButton(
                            button.getX() + button.getWidth() + 4, // X position
                            button.getY(), // Y position
                            20, // Width
                            20, // Height
                            ICON, // Icon texture
                            12, // Icon width
                            12, // Icon height
                            btn -> net.minecraft.client.Minecraft.getInstance().setScreen(checkpointsScreen) // OnPress
                    );

                    event.addListener(iconButton);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(PlayerEvent.PlayerRespawnEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level != null && minecraft.getSingleplayerServer() != null) {
            // Get the latest checkpoint
            List<CheckpointModel> checkpoints = CheckpointModel.getCheckpoints();

            if (checkpoints.isEmpty()) return;

            CheckpointModel latestCheckpoint = checkpoints.get(0);

            if (latestCheckpoint != null) {
                // Load the latest checkpoint
                CheckpointUtil.loadCheckpoint(latestCheckpoint.fileName);
            }
        }
    }
}
