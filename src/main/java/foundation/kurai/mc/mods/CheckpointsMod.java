package foundation.kurai.mc.mods;
import foundation.kurai.mc.mods.handlers.EventsHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(CheckpointsMod.MODID)
public class CheckpointsMod
{
    public static final String MODID = "checkpoints";

    public CheckpointsMod()
    {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(EventsHandler.class);
    }
}
