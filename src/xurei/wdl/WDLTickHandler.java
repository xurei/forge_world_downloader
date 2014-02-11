package xurei.wdl;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class WDLTickHandler implements ITickHandler
{
	boolean started = false;
	private long tick_count = 0;

	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData)
	{
		final Minecraft mc = Minecraft.getMinecraft();
		tick_count++;
		
		if (mc.currentScreen != null)
		{
			if ( (mc.currentScreen instanceof GuiIngameMenu) && ! (mc.currentScreen instanceof WDLGuiIngameMenuExtend))
			{
				mc.currentScreen = new WDLGuiIngameMenuExtend(mc.currentScreen);
			}
			//System.out.println(mc.currentScreen.getClass().getName());

		}
		/*if (!started)
		{
			mc.ingameGUI = new GuiIngameCustom(mc); // Just put in your custom gui
																							// instead of GuiIngameCustom ;)
			started = true;
		}*/
		
		/*WDL>>>*/
    if( WDL.guiToShowAsync != null )
    {
        mc.displayGuiScreen( WDL.guiToShowAsync );
        WDL.guiToShowAsync = null;
    }
    if( WDL.downloading )
    {
        if( WDL.tp.openContainer != WDL.windowContainer )
        {
            if( WDL.tp.openContainer == WDL.tp.inventoryContainer )
                WDL.onItemGuiClosed();
            else
                WDL.onItemGuiOpened();
            WDL.windowContainer = WDL.tp.openContainer;
        }
    }
    /*<<<WDL*/
	}

	public void tickEnd(EnumSet<TickType> type, Object... tickData)
	{
	}
	
	public long getTickCount()
	{
		return tick_count;
	}

	@Override
	public EnumSet<TickType> ticks()
	{
		return EnumSet.of(TickType.CLIENT);
	}

	@Override
	public String getLabel()
	{
		return "xurei GUI Tick Handler";
	}
}