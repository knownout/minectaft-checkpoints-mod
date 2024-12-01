package foundation.kurai;
import foundation.kurai.handlers.EventsHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(CheckpointsMod.MODID)
public class CheckpointsMod
{
    public static final String MODID = "checkpoints_mod";

    public CheckpointsMod()
    {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(EventsHandler.class);
    }
}
