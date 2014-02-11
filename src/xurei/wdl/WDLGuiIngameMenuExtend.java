package xurei.wdl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;

public class WDLGuiIngameMenuExtend extends GuiIngameMenu
{	
	public WDLGuiIngameMenuExtend(GuiScreen screen)
	{
		super();
		this.setWorldAndResolution(Minecraft.getMinecraft(), screen.width, screen.height);
	}
  
  private final static int BUTTON_DOWNLOAD = 5300;
  private final static int BUTTON_OPTIONS = 5301;
  
  /**
   * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
   */
	@Override
  protected void actionPerformed(GuiButton par1GuiButton)
  {
		super.actionPerformed(par1GuiButton);
		switch (par1GuiButton.id)
		{
			case BUTTON_DOWNLOAD:
			  if( WDL.downloading == true )
			  {
			  	WDL.stop();
			  	System.out.println("Stopped download !");
				}
				else
				{
					WDL.start();
					System.out.println("Started download !");
			  }
			
			  mc.displayGuiScreen(null);
			  mc.setIngameFocus();
			break;
			
			case BUTTON_OPTIONS:
				System.out.println("Options");
			 //mc.displayGuiScreen( new GuiWDL( this ) );
			break;
		}
  }
	
	@Override
	public void initGui()
  {
		super.initGui();
		
		/*WDL>>>*/
	  // This code adds the start, stop and options buttons to the menu:
	  byte height = -16;
	  if( !mc.isIntegratedServerRunning() ) // (If connected to real server)
	  {
	  	int y = ((GuiButton)this.buttonList.get(0)).yPosition;
	  	
		  GuiButton wdlDownload = new GuiButton(BUTTON_DOWNLOAD, width / 2 - 100, y + 30, 170, 20, "WDL bug!");
		  wdlDownload.displayString = (WDL.downloading ? (WDL.saving ? "Still saving..." : "Stop download") : "Download this world");
		  this.buttonList.add(wdlDownload);
		  wdlDownload.enabled = (!WDL.downloading || (WDL.downloading && !WDL.saving));
		  
		  GuiButton wdlOptions = new GuiButton(BUTTON_OPTIONS, width / 2 + 71, y + 30, 28, 20, "...");
		  this.buttonList.add(wdlOptions);
		  wdlOptions.enabled = (!WDL.downloading || (WDL.downloading && !WDL.saving));
		  
		  //((GuiButton)this.buttonList.get(0)).yPosition = height / 4 + 144 + height;
		  //((GuiButton)this.buttonList.get(2)).yPosition = height / 4 + 120 + height;
		  //((GuiButton)this.buttonList.get(3)).yPosition = 0;
    }
    /*<<<WDL*/
  }
}
