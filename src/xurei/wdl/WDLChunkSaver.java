package xurei.wdl;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import xurei.util.Sleep;

import net.minecraft.client.Minecraft;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.IChunkLoader;

/**
 * This is a thread that saves a chunck in memory. Prevents from lag spikes when chunk are no longer necessary   
 * @author xurei
 */
public class WDLChunkSaver extends Thread
{
	private boolean started = false;
	private LinkedList<Chunk> chunksQueue = new LinkedList<Chunk>();
	
//----------------------------------------------------------------------------------------------------------------------
	
	@Override
	public void run()
	{
		chunksQueue = new LinkedList<Chunk>();
		Minecraft mc = Minecraft.getMinecraft();
		
		while(started)
		{
			if (chunksQueue.isEmpty())
			{
				Sleep.sleep(500);
			}
			else
			{
				Chunk c = chunksQueue.poll();
				//long t1 = System.currentTimeMillis();
				WDL.importTileEntities( c );
				c.isTerrainPopulated = true;
				try
				{
					WDL.chunkLoader.saveChunk( WDL.wc, c );
				}
				catch ( Exception e )
				{
					// Better tell the player that something didn't work:
					WDL.chatMsg( "Chunk at chunk position " + c.xPosition + "," + c.zPosition + " can't be saved!" );
					e.printStackTrace();
				}
				//long t2 = System.currentTimeMillis();
				//WDL.chatDebug(Modloader.instance.tickHandler.getTickCount()+":"+(t2-t1)+" ms");
			}
		}
	}
//----------------------------------------------------------------------------------------------------------------------
	
	public boolean isSaving()
	{
		return !chunksQueue.isEmpty();
	}
//----------------------------------------------------------------------------------------------------------------------
	
	@Override
	public void start()
	{
		started = true;
		super.start();
	}
//----------------------------------------------------------------------------------------------------------------------
	
	public void end()
	{
		started = false;
	}
//----------------------------------------------------------------------------------------------------------------------
	
	public synchronized void saveChunk(Chunk c)
	{
		chunksQueue.add(c);
	}
}
