package xurei.wdl;
import java.util.EnumSet;

import net.minecraft.client.settings.KeyBinding;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.Mod;
//import cpw.mods.fml.common.Mod.EventHandler; // used in 1.6.2
import cpw.mods.fml.common.Mod.PreInit;    // used in 1.5.2
import cpw.mods.fml.common.Mod.Init;       // used in 1.5.2
import cpw.mods.fml.common.Mod.PostInit;   // used in 1.5.2
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(modid="xurei.wdl", name="xurei World Download", version="0.0.1")
@NetworkMod(clientSideRequired=true, serverSideRequired=false)
public class Modloader 
{
	// The instance of your mod that Forge uses.
    @Instance(value = "xurei.wdl")
    public static Modloader instance;
        
    public final WDLTickHandler tickHandler = new WDLTickHandler();
    
    //@EventHandler // used in 1.6.2
    @PreInit    // used in 1.5.2
    public void preInit(FMLPreInitializationEvent event) {
            // Stub Method
    }
    
    //@EventHandler // used in 1.6.2
    @Init       // used in 1.5.2
    public void load(FMLInitializationEvent event) {
    	//proxy.registerKeyBindings();
    	TickRegistry.registerTickHandler(tickHandler, Side.CLIENT);
    	NetworkRegistry.instance().registerConnectionHandler(new WDLConnectionHandler());
    	System.err.println("WDL loaded");
    	instance = this;
    }
   
    //@EventHandler // used in 1.6.2
    @PostInit   // used in 1.5.2
    public void postInit(FMLPostInitializationEvent event) {
            // Stub Method
    }
}
