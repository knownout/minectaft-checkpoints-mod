package foundation.kurai.mc.mods.widgets;
import foundation.kurai.mc.mods.CheckpointModel;
import foundation.kurai.mc.mods.renderers.CheckpointEntityRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CheckpointScrollWidget extends AbstractSelectionList<CheckpointScrollWidget.CheckpointEntry> {
    private CheckpointEntry selectedCheckpoint;

    private Consumer<CheckpointEntry> selectionCallback;

    private Consumer<CheckpointEntry> loadCallback;

    public final List<CheckpointScrollWidget.CheckpointEntry> checkpoints = new ArrayList<>();

    public CheckpointScrollWidget(Minecraft minecraft, int width, int height, int top, int itemHeight) {
        super(minecraft, width, height, top, itemHeight);
    }

    public void setSelectionCallback(Consumer<CheckpointEntry> callback) {
        this.selectionCallback = callback;
    }

    public void setLoadCallback(Consumer<CheckpointEntry> callback) {
        this.loadCallback = callback;
    }

    public int getRowLeft() {
        return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
    }

    public void setCheckpoints(List<CheckpointEntry> checkpoints) {
        this.clearEntries();
        for (CheckpointEntry checkpoint : checkpoints) this.addEntry(checkpoint);
    }

    public CheckpointEntry getSelectedCheckpoint() {
        return this.selectedCheckpoint;
    }

    public List<CheckpointScrollWidget.CheckpointEntry> getCheckpoints() {
        this.checkpoints.clear();

        List<CheckpointModel> checkpointModels = CheckpointModel.getCheckpoints();
        checkpointModels.forEach(checkpointModel -> this.checkpoints.add(new CheckpointEntry(checkpointModel, this)));

        return this.checkpoints;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getWidth() - 6;
    }

    @Override
    public int getRowWidth() {
        return 320;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput p_259858_) {}

    public static class CheckpointEntry extends AbstractSelectionList.Entry<CheckpointEntry> {
        public final CheckpointScrollWidget parentWidget;
        public final CheckpointEntityRenderer renderer;

        private long lastClickTime;

        public CheckpointEntry(CheckpointModel model, CheckpointScrollWidget parentWidget) {
            this.renderer = new CheckpointEntityRenderer(model);

            this.parentWidget = parentWidget;
        }

        @Override
        public void render(
                @NotNull GuiGraphics graphics,
                int index,
                int y,
                int x,
                int rowWidth,
                int rowHeight,
                int mouseX,
                int mouseY,
                boolean isHovered,
                float partialTick
        ) {
            this.renderer.render(graphics, y, x, rowHeight, mouseX, isHovered);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.parentWidget.selectedCheckpoint = this;

            if (this.parentWidget.selectionCallback != null) this.parentWidget.selectionCallback.accept(this);

            if (Util.getMillis() - this.lastClickTime < 250L || mouseX - this.parentWidget.getRowLeft() < 60) {
                if (this.parentWidget.loadCallback != null) this.parentWidget.loadCallback.accept(this);
            } else this.lastClickTime = Util.getMillis();
            return true;
        }
    }
}