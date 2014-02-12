package xurei.wdl;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Properties;

import xurei.util.Reflexion;

import net.minecraft.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockNote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.mco.McoServer;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBrewingStand;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.INetworkManager;
import net.minecraft.tileentity.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.ThreadedFileIOBase;

/**
 * This is the main class that does most of the work.
 */
public class WDL
{
	public static boolean DEBUG = false; // Setting to false will supress debug output in chat console
	// References:
	public static Minecraft   mc; // Reference to the Minecraft object
	public static net.minecraft.client.multiplayer.WorldClient wc; // Reference to the World object that WDL uses
	public static INetworkManager nm = null; // Reference to a connection specific object. Used to detect a new connection.
	public static EntityClientPlayerMP tp;
	public static McoServer mcos;
	
	public static Container windowContainer; // Reference to the place where all the item stacks end up after receiving them.
	public static int lastX = 0, lastY = 0, lastZ = 0; // Last right clicked block. Needed for TileEntity creation!
	public static Entity lastEntity; // Last entity clicked (used for non-block tiles like minecarts with chests)
	
	public static SaveHandler  saveHandler; // For player files and the level.dat file
	public static IChunkLoader chunkLoader; // For the chunks (despite it's name it does also SAVE them)
	
	// Positions of newly created TileEntities that will overwrite the imported ones when saving:
	public static HashSet<ChunkPosition> newTileEntities = new HashSet<ChunkPosition>();
	
	public static GuiScreen guiToShowAsync = null; // A Gui to show in the next world tick. Needed so that the mouse works.
	
	// State variables:
	public static boolean downloading   = false; // Read-only outside of this class!
	public static boolean isMultiworld  = false; // Is this a multiworld server?
	public static boolean propsFound	= false; // Are there saved properties available?
	public static boolean startOnChange = false; // Automatically restart after world changes?
	
	public static boolean saving = false;
	public static boolean worldLoadingDeferred = false;
	
	// Names:
	public static String worldName	  = "WorldDownloaderERROR"; // safe default
	public static String baseFolderName = "WorldDownloaderERROR"; // safe default
	
	// Properties:
	public static Properties baseProps;
	public static Properties worldProps;
	public static Properties defaultProps;
	
	public static WDLChunkSaver chunkSaver = new WDLChunkSaver();
	
	// Initialization:
	static
	{
		// Starts the chunk Saver 
		chunkSaver.start();
		
		// Get the static Minecraft reference:
		mc = (Minecraft) Reflexion.stealField( Minecraft.class, Minecraft.class ).get(Minecraft.class);
		
		// Initialize the Properties template:
		defaultProps = new Properties();
		defaultProps.setProperty("ServerName", "");
		defaultProps.setProperty("WorldName", "");
		defaultProps.setProperty("LinkedWorlds", "");
		defaultProps.setProperty("AutoStart", "false");
		defaultProps.setProperty("Backup", "off");
		defaultProps.setProperty("BackupPath", ""); // Represents folder or zip-file name
		defaultProps.setProperty("BackupsToKeep", "1");
		defaultProps.setProperty("BackupCommand", "");
		defaultProps.setProperty("GameType", "keep");
		defaultProps.setProperty("Time", "keep");
		defaultProps.setProperty("Weather", "keep");
		defaultProps.setProperty("MapFeatures", "false");
		defaultProps.setProperty("RandomSeed", "");
		defaultProps.setProperty("GeneratorName", "default");
		defaultProps.setProperty("GeneratorVersion", "0");
		defaultProps.setProperty("Spawn", "player");
		defaultProps.setProperty("SpawnX", "8");
		defaultProps.setProperty("SpawnY", "127");
		defaultProps.setProperty("SpawnZ", "8");
		defaultProps.setProperty("PlayerPos", "keep");
		defaultProps.setProperty("PlayerX", "8");
		defaultProps.setProperty("PlayerY", "127");
		defaultProps.setProperty("PlayerZ", "8");
		defaultProps.setProperty("PlayerHealth", "20");
		defaultProps.setProperty("PlayerFood", "20");
		
		baseProps = new Properties(defaultProps);
		worldProps = new Properties(baseProps);
	}
	
	
	//// Methods to control World Downloader \\\\
	
	/** Starts the download */
	public static void start( )
	{
		chatMsg("Starting download...");
		wc = mc.theWorld;
		if( isMultiworld && worldName.isEmpty() )
		{
			chatDebug("Multiworld 1");
			//TODO Multiworld
			// Ask the user which world is loaded
			//guiToShowAsync = new GuiWDLMultiworldSelect( null );
			return;
		}

		//TODO remove this line
		propsFound = true;
		
		if( ! propsFound )
		{
			chatDebug("Multiworld 2");
			//TODO Multiworld
			// Never seen this world before. Ask user about multiworlds:
			//guiToShowAsync = new GuiWDLMultiworld( null );
			return;
		}
		worldProps = loadWorldProps( worldName );
		
		saveHandler = (SaveHandler) mc.getSaveLoader().getSaveLoader( getWorldFolderName(worldName), true );
		chunkLoader = saveHandler.getChunkLoader( wc.provider );
		
		newTileEntities = new HashSet<ChunkPosition>();
		
		if( baseProps.getProperty( "ServerName" ).isEmpty() )
			baseProps.setProperty( "ServerName", getServerName() );
		
		startOnChange = true;
		downloading = true;
		chatMsg( "Download started" );
	}
	
	/** Stops the download */
	public static void stop( )
	{
		if(downloading)
		{
			// Indicate that downloading has stopped
			downloading = false;
			startOnChange = false;
			chatMsg( "Download stopped" );
			
			startSaveThread();
		}
	}
	
	private static void startSaveThread()
	{
		// Indicate that we are saving
		WDL.chatMsg( "Save started." );
		WDL.saving = true;
		WDLSaveAsync saver = new WDLSaveAsync();
		Thread thread = new Thread(saver, "WDL Save Thread");
		thread.start();		
	}
	
	//// Callback methods for important events. Call them from suitable locations in the base classes. \\\\
	
	/** Must be called after the static World object in Minecraft has been replaced */
	public static void onWorldLoad( )
	{
		if( mc.isIntegratedServerRunning() )
		{
			chatDebug("Integrated server running, aborting");
			return;
		}
		
		// If already downloading
		if( downloading )
		{
			// If not currently saving, stop the current download and start saving now
			if(!saving)
			{
				WDL.chatMsg("World change detected. Download will start once current save completes.");
				//worldLoadingDeferred = true;
				startSaveThread();
			}
			return;
		}
		loadWorld();
	}
	
	public static void loadWorld()
	{   
		worldName = ""; // The new (multi-)world name is unknown at the moment
		wc = mc.theWorld;
		tp = mc.thePlayer;
		windowContainer = tp.openContainer;
		
		// Is this a different server?
		INetworkManager newNM = tp.sendQueue.getNetManager();
		
		if( nm != newNM )
		{
			// Different server, different world!
			chatDebug( "onWorldLoad: different server!" );
			nm = newNM;
			loadBaseProps();
			if( baseProps.getProperty( "AutoStart" ).equals( "true" ) )
				start();
			else
				startOnChange = false;
		}
		else
		{
			// Same server, different world!
			chatDebug( "onWorldLoad: same server!" );
			if( startOnChange )
				start();
		}
		
		// Hacking the LongHashMap to keep a list of loaded chunks visible
		// Using reflexion is not possible because the LongHashMapEntry class is private
		Reflexion.StolenField longHashMapField = Reflexion.stealField( ChunkProviderClient.class, LongHashMap.class );
		LongHashMap lhm = (LongHashMap) longHashMapField.get(wc.getChunkProvider());
		longHashMapField.set(wc.getChunkProvider(), new WDLLongHashMapWrapper(lhm));
	}
	
	/** Must be called when the world is no longer used */
	public static void onWorldUnload( )
	{
	}
	
	public static void onSaveComplete()
	{
		WDL.mc.getSaveLoader().flushCache();
		WDL.saveHandler.flush();
		WDL.wc = null;
		
		// If still downloading, load the current world and keep on downloading 
		if(downloading)
		{
			WDL.chatMsg( "Save complete. Starting download again." );
			WDL.loadWorld();
			return;
		}
		
		WDL.chatMsg( "Save complete. Your single player file is ready to play!" );
	}
	
	
	/** Must be called when a chunk is no longer needed and should be removed */
	public static void onChunkNoLongerNeeded( Chunk unneededChunk )
	{
		if( unneededChunk == null /*|| unneededChunk.isModified == false*/ )
			return;
		
		//chatDebug( "onChunkNoLongerNeeded: " + unneededChunk.xPosition + ", " + unneededChunk.zPosition );
		saveChunk( unneededChunk );
	}
	
	/** Must be called when a GUI that receives item stacks from the server is shown */
	public static void onItemGuiOpened( )
	{
		if( mc.objectMouseOver == null ) //Sans doute pour détecter si c'est un inventaire
			return;
		
		if( mc.objectMouseOver.typeOfHit == EnumMovingObjectType.ENTITY)
		{
			lastEntity = mc.objectMouseOver.entityHit;
		}
		else
		{
			lastEntity = null;
			lastX = mc.objectMouseOver.blockX;
			lastY = mc.objectMouseOver.blockY;
			lastZ = mc.objectMouseOver.blockZ;
		}
	}
	
	/** Must be called when a GUI that triggered an onItemGuiOpened is no longer shown */
	public static void onItemGuiClosed( )
	{
		String saveName = "";
		
		// If the last thing clicked was an ENTITY
		if(lastEntity != null)
		{
			if(lastEntity instanceof EntityMinecart && windowContainer instanceof ContainerChest)
			{
				EntityMinecart emc = (EntityMinecart)lastEntity;
				if(emc instanceof EntityMinecartChest)
				{
					EntityMinecartChest emcc = (EntityMinecartChest)emc;
					for(int i = 0; i < emcc.getSizeInventory(); i++)
					{
						emcc.setInventorySlotContents(i, windowContainer.getSlot(i).getStack());
						saveName = "Storage Minecart contents";
					}
				}
			}
			else if(lastEntity instanceof EntityVillager && windowContainer instanceof ContainerMerchant)
			{
				EntityVillager ev = (EntityVillager)lastEntity;
				WDL.chatDebug("Saving villager offers is not yet supported.");
				saveName = "Villager offers";
				return;
			}
			else
			{
				//TODO gérer ca d'une façon plus intelligente, là on aura que les coffres
				WDL.chatMsg("Unsupported entity cannot be saved:" + lastEntity.getEntityName());
			}
			WDL.chatDebug("Saved " + saveName + ".");
			return;
		}
		
		// Else, the last thing clicked was a TILE ENTITY
		// Get the tile entity which we are going to update the inventory for 
		TileEntity te = wc.getBlockTileEntity(lastX, lastY, lastZ);
		if(te == null)
		{
			WDL.chatDebug("onItemGuiClosed could not get TE at " + lastX + " " + lastY + " " + lastZ);
			return;
		}
		
		if( windowContainer instanceof ContainerChest && te instanceof TileEntityChest)
		{
			if(windowContainer.inventorySlots.size() > 63 )
			{
				TileEntity te2;
				ChunkPosition cp1 = new ChunkPosition(lastX, lastY, lastZ);
				ChunkPosition cp2;
				TileEntityChest tec1, tec2;
				if((te2 = wc.getBlockTileEntity(lastX, lastY, lastZ+1)) instanceof TileEntityChest)
				{
					tec1 = (TileEntityChest)te;
					tec2 = (TileEntityChest)te2;
					cp2 = new ChunkPosition(lastX, lastY, lastZ+1);
				}
				else if((te2 = wc.getBlockTileEntity(lastX, lastY, lastZ-1)) instanceof TileEntityChest)
				{
					tec1 = (TileEntityChest)te2;
					tec2 = (TileEntityChest)te;
					cp2 = new ChunkPosition(lastX, lastY, lastZ-1);
				}
				else if((te2 = wc.getBlockTileEntity(lastX+1, lastY, lastZ)) instanceof TileEntityChest)
				{
					tec1 = (TileEntityChest)te;
					tec2 = (TileEntityChest)te2;
					cp2 = new ChunkPosition(lastX+1, lastY, lastZ);
				}
				else if((te2 = wc.getBlockTileEntity(lastX-1, lastY, lastZ)) instanceof TileEntityChest)
				{
					tec1 = (TileEntityChest)te2;
					tec2 = (TileEntityChest)te;
					cp2 = new ChunkPosition(lastX-1, lastY, lastZ);
				}
				else
				{
					WDL.chatMsg("Could not save this chest!");
					return;
				}
				copyItemStacks( windowContainer, (TileEntityChest)tec1, 0 );
				copyItemStacks( windowContainer, (TileEntityChest)tec2, 27);
				newTileEntities.add(cp1);
				newTileEntities.add(cp2);
				saveName = "Double Chest contents";
			}
			// basic chest
			else
			{
				copyItemStacks(windowContainer, (TileEntityChest)te, 0);
				newTileEntities.add(new ChunkPosition(lastX, lastY, lastZ));
				saveName = "Chest contents";
			}
		}
		else if( windowContainer instanceof ContainerChest && te instanceof TileEntityEnderChest)
		{
			InventoryEnderChest inventoryEnderChest = tp.getInventoryEnderChest();
			int inventorySize = inventoryEnderChest.getSizeInventory();
			int containerSize = windowContainer.inventorySlots.size();
			for(int i = 0; i < containerSize && i < inventorySize; i++)
			{
				inventoryEnderChest.setInventorySlotContents(i, windowContainer.getSlot(i).getStack());
			}
			saveName = "Ender Chest contents";
		}
		else if( windowContainer instanceof ContainerBrewingStand )
		{
			copyItemStacks( windowContainer, (TileEntityBrewingStand)te, 0);
			newTileEntities.add(new ChunkPosition(lastX, lastY, lastZ));
			saveName = "Brewing Stand contents";
		}
		else if( windowContainer instanceof ContainerDispenser )
		{
			copyItemStacks( windowContainer, (TileEntityDispenser)te, 0);
			newTileEntities.add(new ChunkPosition(lastX, lastY, lastZ));
			saveName = "Dispenser contents";
		}
		else if( windowContainer instanceof ContainerFurnace )
		{
			copyItemStacks( windowContainer, (TileEntityFurnace)te, 0);
			newTileEntities.add(new ChunkPosition(lastX, lastY, lastZ));
			saveName = "Furnace contents";
		}
		else
		{
			WDL.chatDebug("onItemGuiClosed unhandled TE: " + te);
			return;
		}

		WDL.chatDebug("Saved " + saveName + ".");
		return;
	}
	
	 /**
	 * Must be called when a block event is scheduled for the next tick.
	 * The caller has to check if WDL.downloading is true!
	 */
	public static void onBlockEvent( int x, int y, int z, int blockID, int event, int param )
	{
		if( blockID == Block.music.blockID )
		{
			TileEntityNote newTE = new TileEntityNote();
			newTE.note = (byte)( param % 25 );
			wc.setBlockTileEntity( x, y, z, newTE );
			newTileEntities.add( new ChunkPosition( x, y, z ) );
			chatDebug( "onBlockEvent: Note Block: " + x + " " + y + " " + z + " pitch: " + param + " - " + newTE );
		}
		// Pistons, Chests (open, close), EnderChests, ... (see references to WorldServer.addBlockEvent)
	}
	
	
	//// Importing and exporting methods \\\\
	
	/** Load the previously saved TileEntities and add them to the Chunk **/
	public static void importTileEntities( Chunk chunk )
	{
		File chunkSaveLocation = (File) Reflexion.stealField( AnvilChunkLoader.class, File.class ).get(chunkLoader);
		DataInputStream dis = RegionFileCache.getChunkInputStream( chunkSaveLocation, chunk.xPosition, chunk.zPosition );
		try
		{
			NBTTagCompound chunkNBT = CompressedStreamTools.read( dis );
			NBTTagCompound levelNBT = chunkNBT.getCompoundTag( "Level" );
			// The official code checks if the chunk is in the right location. Should I too?.
			NBTTagList tileEntitiesNBT = levelNBT.getTagList( "TileEntities" );
			for( int i = 0; i < tileEntitiesNBT.tagCount(); i++ )
			{
				NBTTagCompound tileEntityNBT = (NBTTagCompound) tileEntitiesNBT.tagAt( i );
				TileEntity te = TileEntity.createAndLoadEntity( tileEntityNBT );
				String entityType = getTEName(te);
				if(( isImportableTileEntity( te ) ))
				{
				  if( ! newTileEntities.contains( new ChunkPosition( te.xCoord, te.yCoord, te.zCoord ) ) )
				  {
				  	wc.setBlockTileEntity( te.xCoord, te.yCoord, te.zCoord, te );
				  	chatDebug("Loaded TE: " + entityType + " at " + te.xCoord + " " + te.yCoord + " " + te.zCoord );
				  }
				  else
					{
				  	chatDebug( "Dropping old TE: " + entityType + " at " + te.xCoord + " " + te.yCoord + " " + te.zCoord );
					}
				}
				else 
				{
					chatDebug( "TE is not importable: " + entityType + " at " + te.xCoord + " " + te.yCoord + " " + te.zCoord );
				}
			}
		}
		catch ( Exception e ) { } // Couldn't load the old chunk. Nothing unusual. Happens with every not downloaded chunk.
	}
	
	/** Checks if the TileEntity should be imported. Only "problematic" TEs will be imported. */
	public static boolean isImportableTileEntity( TileEntity te )
	{
		/*Block block = Block.blocksList[ wc.getBlockId( te.xCoord, te.yCoord, te.zCoord ) ];
		if( block instanceof BlockChest && te instanceof TileEntityChest )
			return true;
		else if( block instanceof BlockDispenser && te instanceof TileEntityDispenser )
			return true;
		else if( block instanceof BlockFurnace && te instanceof TileEntityFurnace )
			return true;
		else if( block instanceof BlockNote && te instanceof TileEntityNote )
			return true;
		else if( block instanceof BlockBrewingStand && te instanceof TileEntityBrewingStand )
			return true;
		else
			return false;*/
		return true;
	}
	
	/** Returns the TileEntity class name. */
	public static String getTEName( TileEntity te )
	{
		Block block = Block.blocksList[ wc.getBlockId( te.xCoord, te.yCoord, te.zCoord ) ];
		return block.getClass().getSimpleName();
	}
	
	/** Saves all remaining chunks, world info and player info. Usually called when stopping. */
	public static void saveEverything( )
	{
		saveProps();
		
		try
		{
			saveHandler.checkSessionLock();
		}
		catch (MinecraftException e)
		{
			throw new RuntimeException( "WorldDownloader: Couldn't get session lock for saving the world!" );
		}

		NBTTagCompound playerNBT = new NBTTagCompound();
		tp.writeToNBT( playerNBT );
		applyOverridesToPlayer( playerNBT );
		
		int version = 19133; wc.getWorldInfo().getSaveVersion();//FIXME Hard coded, because getSaveVersion() is private :-(
		wc.getWorldInfo().setSaveVersion(version);

		NBTTagCompound worldInfoNBT = wc.getWorldInfo().cloneNBTCompound( playerNBT );
		applyOverridesToWorldInfo( worldInfoNBT );
		
		savePlayer( playerNBT );
		saveWorldInfo( worldInfoNBT );
		saveChunks();
	}
	
	/** Save the player (position, health, inventory, ...) into its own file in the players directory */
	public static void savePlayer( NBTTagCompound playerNBT )
	{
		chatDebug( "Saving player data...");
		try
		{
			File playersDirectory = new File( saveHandler.getWorldDirectory(), "players" );
			File playerFile = new File( playersDirectory, tp.username + ".dat.tmp" );
			File playerFileOld = new File( playersDirectory, tp.username + ".dat" );
			
			CompressedStreamTools.writeCompressed( playerNBT, new FileOutputStream( playerFile ) );

			if( playerFileOld.exists() )
				playerFileOld.delete();
			playerFile.renameTo( playerFileOld );
		}
		catch (Exception e)
		{
			throw new RuntimeException( "Couldn't save the player!" );
		}
		chatDebug( "Player data saved.");
	}
	
	/** Save the world metadata (time, gamemode, seed, ...) into the level.dat file */
	public static void saveWorldInfo( NBTTagCompound worldInfoNBT )
	{
		chatDebug( "Saving world metadata...");
		File saveDirectory = saveHandler.getWorldDirectory();
		NBTTagCompound dataNBT = new NBTTagCompound();
		dataNBT.setTag( "Data", worldInfoNBT );
		
		try
		{
			File dataFile = new File( saveDirectory, "level.dat_new" );
			File dataFileBackup = new File( saveDirectory, "level.dat_old" );
			File dataFileOld = new File( saveDirectory, "level.dat" );
			CompressedStreamTools.writeCompressed( dataNBT, new FileOutputStream( dataFile ) );
			
			if( dataFileBackup.exists() )
				dataFileBackup.delete();
			
			dataFileOld.renameTo( dataFileBackup );
			if( dataFileOld.exists() )
				dataFileOld.delete();
			
			dataFile.renameTo( dataFileOld );
			if( dataFile.exists() )
				dataFile.delete();
		}
		catch (Exception e)
		{
			throw new RuntimeException( "Couldn't save the world metadata!" );
		}
		chatDebug( "World data saved.");
	}
	
	/** Calls saveChunk for all currently loaded chunks */
	public static void saveChunks( )
	{
		chatDebug( "Saving chunks...");
		WDLLongHashMapWrapper chunkMapping = (WDLLongHashMapWrapper) Reflexion.stealField( ChunkProviderClient.class, LongHashMap.class ).get(wc.getChunkProvider());
		
		//LongHashMapEntry[] hashArray = (LongHashMapEntry[]) Reflexion.stealField( LongHashMap.class, LongHashMapEntry[].class ).get(chunkMapping);
		
		WDLSaveProgressReporter progressReporter = new WDLSaveProgressReporter();
		progressReporter.start();
		
		/*// Now that we have the HashMap, lets start iterating over it:
		for( LongHashMapEntry lhme : hashArray )
		{
			while( lhme != null )
			{
				Chunk c = (Chunk) lhme.getValue();*/
		for (Chunk c:chunkMapping.getChunks())
		{
				if( c != null /*&& c.isModified*/ ) // only save filled chunks
				{
					saveChunk( c );
				}
				//else
				//	chatMsg( "Didn't save chunk " + c.xPosition + " " + c.zPosition + " because it is null!");
				/*
				lhme = lhme.nextEntry; // Get next Entry in this linked list
			}
		}*/
		}
		
		try
		{
			ThreadedFileIOBase.threadedIOInstance.waitForFinish();
		}
		catch(Exception e)
		{
			chatMsg( "Threw exception waiting for asynchronous IO to finish. Hmmm.");
		}
		chatDebug( "Chunk data saved.");
	}
	
	/**
	 * Renders World Downloader save progress bar
	 */
	/*
	public static void renderSaveProgress()
	{
		if (saveProgress == 0)
			return;
		FontRenderer fontRenderer = mc.fontRenderer;
		ScaledResolution scaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
		int scaledWidth = scaledResolution.getScaledWidth();
		short width = 182;
		int xPos = scaledWidth / 2 - width / 2;
		byte yPos = 12;
		mc.ingameGUI.drawTexturedModalRect(xPos, yPos, 0, 74, width, 5);
		mc.ingameGUI.drawTexturedModalRect(xPos, yPos, 0, 74, width, 5);
		mc.ingameGUI.drawTexturedModalRect(xPos, yPos, 0, 79, saveProgress * width, 5);

		String var9 = "Save Progress";
		fontRenderer.drawStringWithShadow(var9, scaledWidth / 2 - fontRenderer.getStringWidth(var9) / 2, yPos - 10, 16711935);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.renderEngine.getTexture("/gui/icons.png"));
	}
	*/
	
	/** Import all not overwritten TileEntities, then save the chunk */
	public static void saveChunk( Chunk c )
	{
		chunkSaver.saveChunk(c);
	}
	
	
	//// Properties related methods \\\\
	
	/** Loads the server specific set of properties */
	public static void loadBaseProps( )
	{
		baseFolderName = getBaseFolderName();
		baseProps = new Properties( defaultProps );
		try
		{
			baseProps.load( new FileReader( new File( mc.mcDataDir, "saves/" + baseFolderName + "/WorldDownloader.txt" ) ) );
			propsFound = true;
		}
		catch( FileNotFoundException e )
		{
			propsFound = false;
		}
		catch( Exception e )
		{ }
		
		if( baseProps.getProperty("LinkedWorlds").isEmpty() )
		{
			isMultiworld = false;
			worldProps = new Properties( baseProps );
		}
		else
			isMultiworld = true;
	}
	
	/** Loads the world specific set of properties */
	public static Properties loadWorldProps( String theWorldName )
	{
		Properties ret = new Properties( baseProps );
		if( ! theWorldName.isEmpty() )
		{
			String folder = getWorldFolderName( theWorldName );
		
			try { ret.load( new FileReader( new File( mc.mcDataDir, "saves/" + folder + "/WorldDownloader.txt" ) ) ); }
			catch ( Exception e )
			{
				return null;
			}
		}
		return ret;
	}
	
	/** Saves the currently used base and world properties in the corresponding folders */
	public static void saveProps( )
	{
		saveProps( worldName, worldProps );
	}
	
	/** Saves the specified world properties and the base properties in the corresponding folders */
	public static void saveProps( String theWorldName, Properties theWorldProps )
	{
		if( theWorldName.length() > 0 )
		{
			String folder = getWorldFolderName( theWorldName );
			try
			{
				theWorldProps.store( new FileWriter( new File( mc.mcDataDir, "saves/" + folder + "/WorldDownloader.txt" ) ), "" );
			}
			catch ( Exception e )
			{ }
		}
		else if( !isMultiworld )
		{
			baseProps.putAll( theWorldProps );
		}
		
		File baseFolder = new File( mc.mcDataDir, "saves/" + baseFolderName );
		baseFolder.mkdirs();
		try
		{
			baseProps.store( new FileWriter( new File( baseFolder, "WorldDownloader.txt" ) ), "" );
		}
		catch (Exception e)
		{ }
	}
	
	/** Change player specific fields according to the overrides found in the properties file */
	public static void applyOverridesToPlayer( NBTTagCompound playerNBT )
	{
		//Health
		String health = worldProps.getProperty("PlayerHealth");
		if( !health.equals("keep") )
		{
			short h = Short.parseShort(health);
			playerNBT.setShort("Health", h);
		}
		
		//foodLevel, foodTimer, foodSaturationLevel, foodExhaustionLevel
		String food = worldProps.getProperty("PlayerFood");
		if( !food.equals("keep") )
		{
			int f = Integer.parseInt(food);
			playerNBT.setInteger("foodLevel", f);
			playerNBT.setInteger("foodTickTimer", 0);
			if(f == 20)
				playerNBT.setFloat("foodSaturationLevel", 5.0f);
			else
				playerNBT.setFloat("foodSaturationLevel", 0.0f);
			playerNBT.setFloat("foodExhaustionLevel", 0.0f);
		}
		
		//Player Position
		String playerPos = worldProps.getProperty("PlayerPos");
		if( playerPos.equals("xyz") )
		{
			int x = Integer.parseInt(worldProps.getProperty("PlayerX") );
			int y = Integer.parseInt(worldProps.getProperty("PlayerY") );
			int z = Integer.parseInt(worldProps.getProperty("PlayerZ") );
			NBTTagList pos = playerNBT.getTagList("Pos");
			((NBTTagDouble)pos.tagAt(0)).data = x + 0.5;
			((NBTTagDouble)pos.tagAt(1)).data = (double)y + 0.621; //Player height
			((NBTTagDouble)pos.tagAt(2)).data = z + 0.5;
			
			NBTTagList motion = playerNBT.getTagList("Motion");
			((NBTTagDouble)motion.tagAt(0)).data = 0.0;
			((NBTTagDouble)motion.tagAt(1)).data = -0.001; //Needed to land on the ground
			((NBTTagDouble)motion.tagAt(2)).data = 0.0;
			
			NBTTagList rotation = playerNBT.getTagList("Rotation");
			((NBTTagFloat)rotation.tagAt(0)).data = 0.0f;
			((NBTTagFloat)rotation.tagAt(1)).data = 0.0f;
		}
	}
	
	/** Change world and generator specific fields according to the overrides found in the properties file */
	public static void applyOverridesToWorldInfo( NBTTagCompound worldInfoNBT )
	{
		// LevelName
		String baseName = baseProps.getProperty("ServerName");
		String worldName = worldProps.getProperty("WorldName");
		if( worldName.isEmpty() )
			worldInfoNBT.setString("LevelName", baseName);
		else
			worldInfoNBT.setString("LevelName", baseName + " - " + worldName);
		
		// GameType
		String gametypeOption = worldProps.getProperty("GameType");
		if( gametypeOption.equals("keep") )
		{
			if( tp.capabilities.isCreativeMode )
				worldInfoNBT.setInteger("GameType", 1); // Creative
			else
				worldInfoNBT.setInteger("GameType", 0); // Survival
		}
		else if( gametypeOption.equals("survival") )
			worldInfoNBT.setInteger("GameType", 0 );
		else if( gametypeOption.equals("creative") )
			worldInfoNBT.setInteger("GameType", 1);
		else if( gametypeOption.equals("hardcore") )
		{
			worldInfoNBT.setInteger("GameType", 0);
			worldInfoNBT.setBoolean("hardcore", true);
		}
		
		//Time
		String timeOption = worldProps.getProperty("Time");
		if( !timeOption.equals("keep") )
		{
			long t = Integer.parseInt(timeOption);
			worldInfoNBT.setLong("Time", t);
		}
		
		//RandomSeed
		String randomSeed = worldProps.getProperty("RandomSeed");
		long seed = 0;
		if ( !randomSeed.isEmpty() )
		{
			try
			{
				seed = Long.parseLong( randomSeed );
			}
			catch (NumberFormatException numberformatexception)
			{
				seed = randomSeed.hashCode();
			}
		}
		worldInfoNBT.setLong("RandomSeed", seed);
		
		//MapFeatures
		boolean mapFeatures = Boolean.parseBoolean( worldProps.getProperty("MapFeatures") );
		worldInfoNBT.setBoolean("MapFeatures", mapFeatures);
		
		//generatorName
		String generatorName = worldProps.getProperty("GeneratorName");
		worldInfoNBT.setString("generatorName", generatorName);
		
		//generatorVersion
		int generatorVersion = Integer.parseInt( worldProps.getProperty("GeneratorVersion") );
		worldInfoNBT.setInteger("generatorVersion", generatorVersion );
		
		//Weather
		String weather = worldProps.getProperty("Weather");
		if( weather.equals("sunny") )
		{
			worldInfoNBT.setBoolean("raining", false);
			worldInfoNBT.setInteger("rainTime", 0);
			worldInfoNBT.setBoolean("thundering", false);
			worldInfoNBT.setInteger("thunderTime", 0);
		}
		if( weather.equals("rain") )
		{
			worldInfoNBT.setBoolean("raining", true);
			worldInfoNBT.setInteger("rainTime", 24000);
			worldInfoNBT.setBoolean("thundering", false);
			worldInfoNBT.setInteger("thunderTime", 0);
		}
		if( weather.equals("thunderstorm") )
		{
			worldInfoNBT.setBoolean("raining", true);
			worldInfoNBT.setInteger("rainTime", 24000);
			worldInfoNBT.setBoolean("thundering", true);
			worldInfoNBT.setInteger("thunderTime", 24000);
		}
		
		//Spawn
		String spawn = worldProps.getProperty("Spawn");
		if( spawn.equals("player") )
		{
			int x = (int)Math.floor(tp.posX);
			int y = (int)Math.floor(tp.posY);
			int z = (int)Math.floor(tp.posZ);
			worldInfoNBT.setInteger("SpawnX", x);
			worldInfoNBT.setInteger("SpawnY", y);
			worldInfoNBT.setInteger("SpawnZ", z);
			worldInfoNBT.setBoolean("initialized", true);
		}
		else if( spawn.equals("xyz") )
		{
			int x = Integer.parseInt(worldProps.getProperty("SpawnX") );
			int y = Integer.parseInt(worldProps.getProperty("SpawnY") );
			int z = Integer.parseInt(worldProps.getProperty("SpawnZ") );
			worldInfoNBT.setInteger("SpawnX", x);
			worldInfoNBT.setInteger("SpawnY", y);
			worldInfoNBT.setInteger("SpawnZ", z);
			worldInfoNBT.setBoolean("initialized", true);
		}
	}
	
	
	//// Helper methods \\\\
	
	/** Get the name of the server the user specified it in the server list */
	public static String getServerName( )
	{
		try
		{
			if(mc.getServerData() != null)
				return mc.getServerData().serverName;
			else if(mcos != null)
				return "MCRealm: " + URLDecoder.decode(mcos.field_96406_b, "UTF-8");
		}
		catch(Exception e)
		{}
		return "Unidentified Server";
	}
	
	/** Get the base folder name for the server we are connected to */
	public static String getBaseFolderName( )
	{
		return getServerName().replaceAll("\\W+", "_");
	}
	
	/** Get the folder name for the specified world */
	public static String getWorldFolderName( String theWorldName )
	{
		if( theWorldName.isEmpty() )
			return baseFolderName;
		else
			return baseFolderName + " - " + theWorldName;
	}
	
	public static void copyItemStacks( Container c, IInventory i, int startInContainerAt )
	{
		int containerSize = c.inventorySlots.size();
		int inventorySize = i.getSizeInventory();
		int nc = startInContainerAt;
		int ni = 0;
		
		while( (nc < containerSize) && (ni < inventorySize) )
		{
			ItemStack is = c.getSlot( nc ).getStack();
			i.setInventorySlotContents( ni, is );
			ni++;
			nc++;
		}
	}
	
	/** Adds a chat message with a World Downloader prefix */
	public static void chatMsg( String msg )
	{
		//System.out.println( "WorldDownloader: " + msg ); // Just for debugging!
		mc.ingameGUI.getChatGUI().printChatMessage("\u00A7c[WorldDL]\u00A76 " + msg );
	}
	
	/** Adds a chat message with a World Downloader prefix */
	public static void chatDebug( String msg )
	{
		if(!WDL.DEBUG)
			return;
		//System.out.println( "WorldDownloader: " + msg ); // Just for debugging!
	  mc.ingameGUI.getChatGUI().printChatMessage("\u00A72[WorldDL]\u00A76 " + msg );
	}

	public static void handleServerSeedMessage(String msg)
	{
		if(downloading && msg.startsWith("Seed: "))
		{
			String seed = msg.substring(6);
			worldProps.setProperty("RandomSeed", seed);
			WDL.chatMsg("Setting single-player world seed to " + seed);
		}
		/*
		else
		{
			WDL.chatMsg("Could not retrieve server seed");
		}
		*/
	}
}
