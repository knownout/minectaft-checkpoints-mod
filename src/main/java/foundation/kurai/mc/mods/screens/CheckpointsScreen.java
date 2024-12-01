package foundation.kurai.mc.mods.screens;
import foundation.kurai.mc.mods.util.CheckpointUtil;
import foundation.kurai.mc.mods.util.FileSystemUtil;
import foundation.kurai.mc.mods.util.MetadataUtil;
import foundation.kurai.mc.mods.widgets.CheckpointScrollWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class CheckpointsScreen extends Screen {

    private EditBox searchField;
    private Button loadButton;
    private Button deleteButton;
    private Button createButton;
    private CheckpointScrollWidget checkpointList;

    public CheckpointsScreen() {
        super(Component.translatable("screen.checkpoints.checkpoints_list"));
    }

    private void filterCheckpoints(String query) {
        String searchQuery = query.toLowerCase();

        List<CheckpointScrollWidget.CheckpointEntry> filteredCheckpoints = this.checkpointList.checkpoints.stream()
                .filter(entry -> entry.renderer.model.name.toLowerCase().contains(searchQuery)
                ).toList();

        checkpointList.setCheckpoints(filteredCheckpoints);
    }

    // Method to handle input changes in the search field
    private void onSearchFieldChanged(String newText) {
        if (!newText.isEmpty()) {
            this.searchField.setSuggestion(""); // Clear suggestion when user types
            this.createButton.active = true;
        } else {
            this.searchField.setSuggestion(Component.translatable("screen.checkpoints.search_placeholder").getString()); // Restore suggestion if field is empty
            this.createButton.active = false;
        }

        filterCheckpoints(newText);
    }

    @Override
    protected void init() {
        // Initialize checkpoint list
        checkpointList = new CheckpointScrollWidget(
                this.minecraft,
                this.width, // Width of the scroll widget
                this.height - 80, // Height of the scroll widget
                40, // Top margin
                this.height - 40, // Bottom margin
                44 // Height of each entry
        );
        checkpointList.setCheckpoints(this.checkpointList.getCheckpoints()); // Populate with data

        checkpointList.setSelectionCallback(selectedCheckpoint -> {
            boolean hasSelection = selectedCheckpoint != null;

            // Enable or disable buttons based on selection
            this.loadButton.active = hasSelection;
            this.deleteButton.active = hasSelection;
        });

        checkpointList.setLoadCallback(selectedCheckpoint -> {
            boolean hasSelection = selectedCheckpoint != null;

            if (!hasSelection) return;

            onLoadPressed();
        });

        this.addRenderableWidget(checkpointList);

        this.searchField = new EditBox(
                this.font,
                (this.width / 2 - 158), // Centered horizontally
                11, // Position it within the top panel
                210, // Width of the input field
                18, // Height of the input field
                Component.translatable("screen.checkpoints.search_placeholder")
        );
        this.searchField.setMaxLength(50);
        this.searchField.setSuggestion(Component.translatable("screen.checkpoints.search_placeholder").getString());
        this.addRenderableWidget(this.searchField);
        this.searchField.setResponder(this::onSearchFieldChanged);

        this.createButton = Button.builder(
                Component.translatable("screen.checkpoints.create_button"),
                button -> this.onCreatePressed()
        ).bounds(this.width / 2 + 60, 10, 100, 20).build();
        this.createButton.active = false;
        this.addRenderableWidget(this.createButton);

        // Add "Load" button
        this.loadButton = Button.builder(
                Component.translatable("screen.checkpoints.load_button"),
                button -> this.onLoadPressed()
        ).bounds(this.width / 2 - 125, this.height - 30, 80, 20).build();
        this.loadButton.active = false; // Disabled by default
        this.addRenderableWidget(this.loadButton);

        this.deleteButton = Button.builder(
                Component.translatable("screen.checkpoints.delete_button"),
                button -> this.onDeletePressed()
        ).bounds(this.width / 2 - 40, this.height - 30, 80, 20).build();
        this.deleteButton.active = false; // Disabled by default
        this.addRenderableWidget(this.deleteButton);

        Button backButton = Button.builder(
                Component.translatable("screen.checkpoints.back_button"),
                button -> this.onBackPressed()
        ).bounds(this.width / 2 + 45, this.height - 30, 80, 20).build();
        this.addRenderableWidget(backButton);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.searchField.render(graphics, mouseX, mouseY, partialTicks);

//        this.renderBackground(graphics);

        this.checkpointList.render(graphics, mouseX, mouseY, partialTicks);

        renderPanelBackground(graphics, 0, 0.5F);
        renderPanelBackground(graphics, this.height - 40, 0.25F);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    public void renderBackground(GuiGraphics graphics) {
        graphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        graphics.blit(Screen.BACKGROUND_LOCATION, 0, 0, 0, 0, this.width, this.height, 32, 32);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

//        graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }

    private void renderPanelBackground(GuiGraphics graphics, int y, float exp) {
        graphics.setColor(exp, exp, exp, 1.0F);
        graphics.blit(Screen.BACKGROUND_LOCATION, 0, y, 0, 0, this.width, 40, 32, 32);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void onDeletePressed() {
//        if (this.minecraft != null) this.minecraft.setScreen(null); // Close the screen

        // Get the selected checkpoint
        CheckpointScrollWidget.CheckpointEntry selected = checkpointList.getSelectedCheckpoint();
        if (selected == null) {
            System.out.println("No checkpoint selected to delete.");
            return;
        }

        // Confirm directory exists
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() == null) return;

        File worldDirectory = MetadataUtil.getWorldPath().toFile();
        File checkpointsDir = new File(worldDirectory, "checkpoints");
        File checkpointDir = new File(checkpointsDir, selected.renderer.model.fileName);

        if (checkpointDir.exists() && checkpointDir.isDirectory()) {
            // Delete the directory and its contents
            FileSystemUtil.deleteDirectory(checkpointDir, true);
            System.out.println("Deleted checkpoint: " + selected.renderer.model.name);

            // Refresh the checkpoints list
            checkpointList.setCheckpoints(this.checkpointList.getCheckpoints());
            checkpointList.setSelectionCallback(selectedCheckpoint -> {
                boolean hasSelection = selectedCheckpoint != null;
                this.loadButton.active = hasSelection;
                this.deleteButton.active = hasSelection;
            });

            // Disable buttons if no selection
            this.loadButton.active = false;
            this.deleteButton.active = false;
        } else {
            System.out.println("Checkpoint directory does not exist: " + checkpointDir.getAbsolutePath());
        }
    }

    private void onCreatePressed() {
        String name = this.searchField.getValue();

        if (this.searchField.getValue().isEmpty()) return;

        String finalName = name;
        boolean exist = this.checkpointList.checkpoints.stream()
                .anyMatch(entry -> entry.renderer.model.fileName.equalsIgnoreCase(finalName));

        while (exist) {
            name = MetadataUtil.generateNextName(name);
            String finalName1 = name;
            exist = this.checkpointList.checkpoints.stream()
                    .anyMatch(entry -> entry.renderer.model.fileName.equalsIgnoreCase(finalName1));
        }

        CheckpointUtil.createCheckpoint(this.searchField.getValue(), name, System.out::println);
    }

    private void onLoadPressed() {
        CheckpointScrollWidget.CheckpointEntry selected = checkpointList.getSelectedCheckpoint();
        if (selected == null) {
            System.out.println("No checkpoint selected to load.");
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getSingleplayerServer() == null) {
            System.out.println("No world currently loaded.");
            return;
        }

        CheckpointUtil.loadCheckpoint(selected.renderer.model.fileName);
    }

    private void onBackPressed() {
        if (this.minecraft != null) this.minecraft.setScreen(new PauseScreen(true)); // Close the screen
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(new PauseScreen(true));
    }

}