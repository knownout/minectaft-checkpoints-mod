package foundation.kurai.mc.mods.handlers;

import foundation.kurai.mc.mods.CheckpointModel;
import foundation.kurai.mc.mods.CheckpointsMod;
import foundation.kurai.mc.mods.screens.CheckpointsScreen;
import foundation.kurai.mc.mods.util.CheckpointUtil;
import foundation.kurai.mc.mods.widgets.IconButton;
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

@Mod.EventBusSubscriber(modid = CheckpointsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventsHandler {

    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(CheckpointsMod.MODID, "textures/gui/checkpoints_list.png");

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
                            button.getY(),
                            20,
                            20,
                            ICON,
                            12,
                            12,
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

            CheckpointModel latestCheckpoint = checkpoints.getFirst();

            if (latestCheckpoint != null) {
                // Load the latest checkpoint
                CheckpointUtil.loadCheckpoint(latestCheckpoint.fileName);
            }
        }
    }
}
