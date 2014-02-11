package xurei.wdl;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;

/**
 * This wrapper of LongHashMap keeps all the keys in memory. This way we can use getValueByKey
 * for all the keys without having to make LongHashMapEntry public. This makes the save a little
 * bit slower, probably, but it is neglectable.
 * @author xurei
 */
public class WDLLongHashMapWrapper extends LongHashMap {
  
  private final LongHashMap lhm;
  private final Set<Long> added_keys = new TreeSet<Long>();
//----------------------------------------------------------------------------------------------------------------------
  
  public WDLLongHashMapWrapper(LongHashMap _lhm)
  {
  	lhm = _lhm;
  }
//----------------------------------------------------------------------------------------------------------------------
  
  public synchronized void add(long par1, Object par3Obj)
  {
  	lhm.add(par1, par3Obj);
  	added_keys.add(par1);
  }
//----------------------------------------------------------------------------------------------------------------------
  
  public synchronized List<Chunk> getChunks()
  {
  	List<Chunk> out = new LinkedList<Chunk>();
  	for (Long k:added_keys)
  	{
  		out.add((Chunk)lhm.getValueByKey(k));
  	}
  	return out;
  }
//----------------------------------------------------------------------------------------------------------------------
//-------------------------------------------- Wrapper methods ---------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
  
  public boolean containsItem(long par1)
  {
  	return lhm.containsItem(par1);
  }
//----------------------------------------------------------------------------------------------------------------------
  
  public int getNumHashElements()
  {
  	return lhm.getNumHashElements();
  }
//----------------------------------------------------------------------------------------------------------------------
  
  public Object getValueByKey(long par1)
  {
  	return lhm.getValueByKey(par1);
  }
//----------------------------------------------------------------------------------------------------------------------
  
  public Object remove(long par1)
  {
  	added_keys.remove(par1);
  	return lhm.remove(par1);
  }
}
