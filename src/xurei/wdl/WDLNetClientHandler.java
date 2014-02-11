package xurei.wdl;

import java.io.IOException;

import xurei.util.Reflexion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet0KeepAlive;
import net.minecraft.network.packet.Packet100OpenWindow;
import net.minecraft.network.packet.Packet101CloseWindow;
import net.minecraft.network.packet.Packet103SetSlot;
import net.minecraft.network.packet.Packet104WindowItems;
import net.minecraft.network.packet.Packet105UpdateProgressbar;
import net.minecraft.network.packet.Packet106Transaction;
import net.minecraft.network.packet.Packet10Flying;
import net.minecraft.network.packet.Packet130UpdateSign;
import net.minecraft.network.packet.Packet131MapData;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.network.packet.Packet16BlockItemSwitch;
import net.minecraft.network.packet.Packet17Sleep;
import net.minecraft.network.packet.Packet18Animation;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.network.packet.Packet200Statistic;
import net.minecraft.network.packet.Packet201PlayerInfo;
import net.minecraft.network.packet.Packet202PlayerAbilities;
import net.minecraft.network.packet.Packet203AutoComplete;
import net.minecraft.network.packet.Packet206SetObjective;
import net.minecraft.network.packet.Packet207SetScore;
import net.minecraft.network.packet.Packet208SetDisplayObjective;
import net.minecraft.network.packet.Packet209SetPlayerTeam;
import net.minecraft.network.packet.Packet20NamedEntitySpawn;
import net.minecraft.network.packet.Packet22Collect;
import net.minecraft.network.packet.Packet23VehicleSpawn;
import net.minecraft.network.packet.Packet24MobSpawn;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet252SharedKey;
import net.minecraft.network.packet.Packet253ServerAuthData;
import net.minecraft.network.packet.Packet255KickDisconnect;
import net.minecraft.network.packet.Packet25EntityPainting;
import net.minecraft.network.packet.Packet26EntityExpOrb;
import net.minecraft.network.packet.Packet28EntityVelocity;
import net.minecraft.network.packet.Packet29DestroyEntity;
import net.minecraft.network.packet.Packet30Entity;
import net.minecraft.network.packet.Packet34EntityTeleport;
import net.minecraft.network.packet.Packet35EntityHeadRotation;
import net.minecraft.network.packet.Packet38EntityStatus;
import net.minecraft.network.packet.Packet39AttachEntity;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.network.packet.Packet40EntityMetadata;
import net.minecraft.network.packet.Packet41EntityEffect;
import net.minecraft.network.packet.Packet42RemoveEntityEffect;
import net.minecraft.network.packet.Packet43Experience;
import net.minecraft.network.packet.Packet4UpdateTime;
import net.minecraft.network.packet.Packet51MapChunk;
import net.minecraft.network.packet.Packet52MultiBlockChange;
import net.minecraft.network.packet.Packet53BlockChange;
import net.minecraft.network.packet.Packet54PlayNoteBlock;
import net.minecraft.network.packet.Packet55BlockDestroy;
import net.minecraft.network.packet.Packet56MapChunks;
import net.minecraft.network.packet.Packet5PlayerInventory;
import net.minecraft.network.packet.Packet60Explosion;
import net.minecraft.network.packet.Packet61DoorChange;
import net.minecraft.network.packet.Packet62LevelSound;
import net.minecraft.network.packet.Packet63WorldParticles;
import net.minecraft.network.packet.Packet6SpawnPosition;
import net.minecraft.network.packet.Packet70GameEvent;
import net.minecraft.network.packet.Packet71Weather;
import net.minecraft.network.packet.Packet8UpdateHealth;
import net.minecraft.network.packet.Packet9Respawn;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.stats.StatList;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

/**
 * Wrapper of NetClientHandler, which overrides the handling of packets 51 and 56. A decorator of an observer pattern 
 * would be better, but forge does not allow that.
 * @author xurei
 */
@SideOnly(Side.CLIENT)
public class WDLNetClientHandler extends NetHandler//extends NetClientHandler
{	
	NetClientHandler nch;
	WorldClient wc = null;
//----------------------------------------------------------------------------------------------------------------------
	
	public WDLNetClientHandler(NetClientHandler _nch)
	{		
		nch = _nch;
	}
//----------------------------------------------------------------------------------------------------------------------
	
	private void doPreChunk(int x, int z, boolean needed)
	{
		if (needed)
		{
			//System.out.println("Prechunk needed "+x+" "+z);
		  /*WDL>>>*/ // doPreChunk() World change
		  if( wc != WDL.wc ) //World change
		  	WDL.onWorldLoad();
		  /*<<<WDL*/
		}
		else
		{
			//System.out.println("Prechunk not needed "+x+" "+z);
		  /*WDL>>>*/ // doPreChunk() Chunk no longer needed => warn downloader
		  if( WDL.downloading ) //Chunk no longer needed => warn downloader
		  	WDL.onChunkNoLongerNeeded( wc.getChunkProvider().provideChunk(x, z) );
		  /*<<<WDL*/
		}

		wc.doPreChunk(x, z, needed);
	}
//----------------------------------------------------------------------------------------------------------------------
	
	public void handleMapChunk(Packet51MapChunk mapChunk)
	{
		if (wc == null)
		{
			wc = (WorldClient)Reflexion.stealField(NetClientHandler.class, WorldClient.class).get(nch);
		}
		
		if (mapChunk.includeInitialize)
		{
			if (mapChunk.yChMin == 0)
			{
				doPreChunk(mapChunk.xCh, mapChunk.zCh, false);
				return;
			}
	
			doPreChunk(mapChunk.xCh, mapChunk.zCh, true);
		}
	
		wc.invalidateBlockReceiveRegion(mapChunk.xCh << 4, 0, mapChunk.zCh << 4, (mapChunk.xCh << 4) + 15, 256, (mapChunk.zCh << 4) + 15);
		Chunk chunk = wc.getChunkFromChunkCoords(mapChunk.xCh, mapChunk.zCh);
	
		if (mapChunk.includeInitialize && chunk == null)
		{
			doPreChunk(mapChunk.xCh, mapChunk.zCh, true);
			chunk = wc.getChunkFromChunkCoords(mapChunk.xCh, mapChunk.zCh);
		}
	
		if (chunk != null)
		{
			chunk.fillChunk(mapChunk.getCompressedChunkData(), mapChunk.yChMin, mapChunk.yChMax, mapChunk.includeInitialize);
			wc.markBlockRangeForRenderUpdate(mapChunk.xCh << 4, 0, mapChunk.zCh << 4, (mapChunk.xCh << 4) + 15, 256, (mapChunk.zCh << 4) + 15);
	
			if (!mapChunk.includeInitialize || !(wc.provider instanceof WorldProviderSurface))
			{
				chunk.resetRelightChecks();
			}
		}
	}
//----------------------------------------------------------------------------------------------------------------------
	
	@Override
	public void handleMapChunks(Packet56MapChunks par1Packet56MapChunks)
	{
		if (wc == null)
		{
			wc = (WorldClient)Reflexion.stealField(NetClientHandler.class, WorldClient.class).get(nch);
		}
		
		//nch.handleMapChunks(par1Packet56MapChunks);
		for (int i = 0; i < par1Packet56MapChunks.getNumberOfChunkInPacket(); ++i)
		{
			int j = par1Packet56MapChunks.getChunkPosX(i);
			int k = par1Packet56MapChunks.getChunkPosZ(i);
			doPreChunk(j, k, true);
			wc.invalidateBlockReceiveRegion(j << 4, 0, k << 4, (j << 4) + 15, 256, (k << 4) + 15);
			Chunk chunk = wc.getChunkFromChunkCoords(j, k);

			if (chunk == null)
			{
				doPreChunk(j, k, true);
				chunk = wc.getChunkFromChunkCoords(j, k);
			}

			if (chunk != null)
			{
				chunk.fillChunk(par1Packet56MapChunks.getChunkCompressedData(i), par1Packet56MapChunks.field_73590_a[i], par1Packet56MapChunks.field_73588_b[i], true);
				wc.markBlockRangeForRenderUpdate(j << 4, 0, k << 4, (j << 4) + 15, 256, (k << 4) + 15);

				if (!(wc.provider instanceof WorldProviderSurface))
				{
						chunk.resetRelightChecks();
				}
			}
		}
	}
//----------------------------------------------------------------------------------------------------------------------
	
	@Override
	public void handleKickDisconnect(Packet255KickDisconnect par1Packet255KickDisconnect)
	{
    /* WDL >>> */
    if(WDL.downloading)
    {
        WDL.stop();
        try{Thread.sleep(2000);}catch(Exception e){}
    }
    /* <<< WDL */
		nch.handleKickDisconnect(par1Packet255KickDisconnect);		
	}
//----------------------------------------------------------------------------------------------------------------------

	public void handleErrorMessage(String par1Str, Object[] par2ArrayOfObj)
	{
    /* WDL >>> */
    if(WDL.downloading)
    {
        WDL.stop();
        try{Thread.sleep(2000);}catch(Exception e){}
    }
    /* <<< WDL */
    
		nch.handleErrorMessage(par1Str, par2ArrayOfObj);		
	}
//----------------------------------------------------------------------------------------------------------------------
	
	public void cleanup()
	{
		nch.cleanup();
	}
	public void processReadPackets()
	{
		nch.processReadPackets();
	}
	@Override
	public void handleServerAuthData(Packet253ServerAuthData par1Packet253ServerAuthData)
	{
		nch.handleServerAuthData(par1Packet253ServerAuthData);
		
	}
	@Override
	public void handleSharedKey(Packet252SharedKey par1Packet252SharedKey)
	{
		nch.handleSharedKey(par1Packet252SharedKey);
		
	}
	@Override
	public void handleLogin(Packet1Login par1Packet1Login)
	{
		nch.handleLogin(par1Packet1Login);
		
	}
	@Override
	public void handleVehicleSpawn(Packet23VehicleSpawn par1Packet23VehicleSpawn)
	{
		nch.handleVehicleSpawn(par1Packet23VehicleSpawn);
		
	}
	@Override
	public void handleEntityExpOrb(Packet26EntityExpOrb par1Packet26EntityExpOrb)
	{
		nch.handleEntityExpOrb(par1Packet26EntityExpOrb);
		
	}
	@Override
	public void handleWeather(Packet71Weather par1Packet71Weather)
	{
		nch.handleWeather(par1Packet71Weather);
		
	}
	@Override
	public void handleEntityPainting(Packet25EntityPainting par1Packet25EntityPainting)
	{
		nch.handleEntityPainting(par1Packet25EntityPainting);
		
	}
	@Override
	public void handleEntityVelocity(Packet28EntityVelocity par1Packet28EntityVelocity)
	{
		nch.handleEntityVelocity(par1Packet28EntityVelocity);
		
	}
	@Override
	public void handleEntityMetadata(Packet40EntityMetadata par1Packet40EntityMetadata)
	{
		nch.handleEntityMetadata(par1Packet40EntityMetadata);
		
	}
	@Override
	public void handleNamedEntitySpawn(Packet20NamedEntitySpawn par1Packet20NamedEntitySpawn)
	{
		nch.handleNamedEntitySpawn(par1Packet20NamedEntitySpawn);		
	}
	@Override
	public void handleEntityTeleport(Packet34EntityTeleport par1Packet34EntityTeleport)
	{
		nch.handleEntityTeleport(par1Packet34EntityTeleport);		
	}
	@Override
	public void handleBlockItemSwitch(Packet16BlockItemSwitch par1Packet16BlockItemSwitch)
	{
		nch.handleBlockItemSwitch(par1Packet16BlockItemSwitch);		
	}
	@Override
	public void handleEntity(Packet30Entity par1Packet30Entity)
	{
		nch.handleEntity(par1Packet30Entity);		
	}
	@Override
	public void handleEntityHeadRotation(Packet35EntityHeadRotation par1Packet35EntityHeadRotation)
	{
		nch.handleEntityHeadRotation(par1Packet35EntityHeadRotation);		
	}
	@Override
	public void handleDestroyEntity(Packet29DestroyEntity par1Packet29DestroyEntity)
	{
		nch.handleDestroyEntity(par1Packet29DestroyEntity);		
	}
	@Override
	public void handleFlying(Packet10Flying par1Packet10Flying)
	{
		nch.handleFlying(par1Packet10Flying);		
	}
	@Override
	public void handleMultiBlockChange(Packet52MultiBlockChange par1Packet52MultiBlockChange)
	{
		nch.handleMultiBlockChange(par1Packet52MultiBlockChange);		
	}
	@Override
	public void handleBlockChange(Packet53BlockChange par1Packet53BlockChange)
	{
		nch.handleBlockChange(par1Packet53BlockChange);		
	}
	public void quitWithPacket(Packet par1Packet)
	{
		nch.quitWithPacket(par1Packet);		
	}
	public void addToSendQueue(Packet par1Packet)
	{
		nch.addToSendQueue(par1Packet);		
	}
	@Override
	public void handleCollect(Packet22Collect par1Packet22Collect)
	{
		nch.handleCollect(par1Packet22Collect);		
	}
	@Override
	public void handleChat(Packet3Chat par1Packet3Chat)
	{
		nch.handleChat(par1Packet3Chat);		
	}
	@Override
	public void handleAnimation(Packet18Animation par1Packet18Animation)
	{
		nch.handleAnimation(par1Packet18Animation);		
	}
	@Override
	public void handleSleep(Packet17Sleep par1Packet17Sleep)
	{
		nch.handleSleep(par1Packet17Sleep);		
	}
	public void disconnect()
	{
		nch.disconnect();		
	}
	@Override
	public void handleMobSpawn(Packet24MobSpawn par1Packet24MobSpawn)
	{
		nch.handleMobSpawn(par1Packet24MobSpawn);		
	}
	@Override
	public void handleUpdateTime(Packet4UpdateTime par1Packet4UpdateTime)
	{
		nch.handleUpdateTime(par1Packet4UpdateTime);		
	}
	@Override
	public void handleSpawnPosition(Packet6SpawnPosition par1Packet6SpawnPosition)
	{
		nch.handleSpawnPosition(par1Packet6SpawnPosition);		
	}
	@Override
	public void handleAttachEntity(Packet39AttachEntity par1Packet39AttachEntity)
	{
		nch.handleAttachEntity(par1Packet39AttachEntity);		
	}
	@Override
	public void handleEntityStatus(Packet38EntityStatus par1Packet38EntityStatus)
	{
		nch.handleEntityStatus(par1Packet38EntityStatus);		
	}
	@Override
	public void handleUpdateHealth(Packet8UpdateHealth par1Packet8UpdateHealth)
	{
		nch.handleUpdateHealth(par1Packet8UpdateHealth);		
	}
	@Override
	public void handleExperience(Packet43Experience par1Packet43Experience)
	{
		nch.handleExperience(par1Packet43Experience);		
	}
	@Override
	public void handleRespawn(Packet9Respawn par1Packet9Respawn)
	{
		nch.handleRespawn(par1Packet9Respawn);		
	}
	@Override
	public void handleExplosion(Packet60Explosion par1Packet60Explosion)
	{
		nch.handleExplosion(par1Packet60Explosion);		
	}
	@Override
	public void handleOpenWindow(Packet100OpenWindow par1Packet100OpenWindow)
	{
		nch.handleOpenWindow(par1Packet100OpenWindow);		
	}
	@Override
	public void handleSetSlot(Packet103SetSlot par1Packet103SetSlot)
	{
		nch.handleSetSlot(par1Packet103SetSlot);		
	}
	@Override
	public void handleTransaction(Packet106Transaction par1Packet106Transaction)
	{
		nch.handleTransaction(par1Packet106Transaction);		
	}
	@Override
	public void handleWindowItems(Packet104WindowItems par1Packet104WindowItems)
	{
		nch.handleWindowItems(par1Packet104WindowItems);		
	}
	@Override
	public void handleUpdateSign(Packet130UpdateSign par1Packet130UpdateSign)
	{
		nch.handleUpdateSign(par1Packet130UpdateSign);		
	}
	@Override
	public void handleTileEntityData(Packet132TileEntityData par1Packet132TileEntityData)
	{
		nch.handleTileEntityData(par1Packet132TileEntityData);		
	}
	@Override
	public void handleUpdateProgressbar(Packet105UpdateProgressbar par1Packet105UpdateProgressbar)
	{
		nch.handleUpdateProgressbar(par1Packet105UpdateProgressbar);		
	}
	@Override
	public void handlePlayerInventory(Packet5PlayerInventory par1Packet5PlayerInventory)
	{
		nch.handlePlayerInventory(par1Packet5PlayerInventory);		
	}
	@Override
	public void handleCloseWindow(Packet101CloseWindow par1Packet101CloseWindow)
	{
		nch.handleCloseWindow(par1Packet101CloseWindow);		
	}
	@Override
	public void handleBlockEvent(Packet54PlayNoteBlock par1Packet54PlayNoteBlock)
	{
		nch.handleBlockEvent(par1Packet54PlayNoteBlock);		
	}
	@Override
	public void handleBlockDestroy(Packet55BlockDestroy par1Packet55BlockDestroy)
	{
		nch.handleBlockDestroy(par1Packet55BlockDestroy);		
	}
	@Override
	public boolean canProcessPacketsAsync()
	{
		return nch.canProcessPacketsAsync();
	}
	@Override
	public void handleGameEvent(Packet70GameEvent par1Packet70GameEvent)
	{
		nch.handleGameEvent(par1Packet70GameEvent);		
	}
	@Override
	public void handleMapData(Packet131MapData par1Packet131MapData)
	{
		nch.handleMapData(par1Packet131MapData);		
	}
	public void fmlPacket131Callback(Packet131MapData par1Packet131MapData)
	{
		nch.fmlPacket131Callback(par1Packet131MapData);		
	}
	@Override
	public void handleDoorChange(Packet61DoorChange par1Packet61DoorChange)
	{
		nch.handleDoorChange(par1Packet61DoorChange);		
	}
	@Override
	public void handleStatistic(Packet200Statistic par1Packet200Statistic)
	{
		nch.handleStatistic(par1Packet200Statistic);		
	}
	@Override
	public void handleEntityEffect(Packet41EntityEffect par1Packet41EntityEffect)
	{
		nch.handleEntityEffect(par1Packet41EntityEffect);		
	}
	@Override
	public void handleRemoveEntityEffect(Packet42RemoveEntityEffect par1Packet42RemoveEntityEffect)
	{
		nch.handleRemoveEntityEffect(par1Packet42RemoveEntityEffect);		
	}
	@Override
	public boolean isServerHandler()
	{
		return nch.isServerHandler();
	}
	@Override
	public void handlePlayerInfo(Packet201PlayerInfo par1Packet201PlayerInfo)
	{
		nch.handlePlayerInfo(par1Packet201PlayerInfo);		
	}
	@Override
	public void handleKeepAlive(Packet0KeepAlive par1Packet0KeepAlive)
	{
		nch.handleKeepAlive(par1Packet0KeepAlive);		
	}
	@Override
	public void handlePlayerAbilities(Packet202PlayerAbilities par1Packet202PlayerAbilities)
	{
		nch.handlePlayerAbilities(par1Packet202PlayerAbilities);		
	}
	@Override
	public void handleAutoComplete(Packet203AutoComplete par1Packet203AutoComplete)
	{
		nch.handleAutoComplete(par1Packet203AutoComplete);		
	}
	@Override
	public void handleLevelSound(Packet62LevelSound par1Packet62LevelSound)
	{
		nch.handleLevelSound(par1Packet62LevelSound);		
	}
	@Override
	public void handleCustomPayload(Packet250CustomPayload par1Packet250CustomPayload)
	{
		nch.handleCustomPayload(par1Packet250CustomPayload);		
	}
	@Override
	public void handleVanilla250Packet(Packet250CustomPayload par1Packet250CustomPayload)
	{
		nch.handleVanilla250Packet(par1Packet250CustomPayload);		
	}
	@Override
	public void handleSetObjective(Packet206SetObjective par1Packet206SetObjective)
	{
		nch.handleSetObjective(par1Packet206SetObjective);		
	}
	@Override
	public void handleSetScore(Packet207SetScore par1Packet207SetScore)
	{
		nch.handleSetScore(par1Packet207SetScore);		
	}
	@Override
	public void handleSetDisplayObjective(Packet208SetDisplayObjective par1Packet208SetDisplayObjective)
	{
		nch.handleSetDisplayObjective(par1Packet208SetDisplayObjective);		
	}
	@Override
	public void handleSetPlayerTeam(Packet209SetPlayerTeam par1Packet209SetPlayerTeam)
	{
		nch.handleSetPlayerTeam(par1Packet209SetPlayerTeam);		
	}
	@Override
	public void handleWorldParticles(Packet63WorldParticles par1Packet63WorldParticles)
	{
		nch.handleWorldParticles(par1Packet63WorldParticles);		
	}
	public INetworkManager getNetManager()
	{
		return nch.getNetManager();
	}
	@Override
	public EntityPlayer getPlayer()
	{
		return nch.getPlayer();
	}
}
