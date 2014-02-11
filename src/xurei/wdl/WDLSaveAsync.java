package xurei.wdl;
import xurei.util.Sleep;

public class WDLSaveAsync implements Runnable
{
    public void run()
    {
    	// Save everything
        WDL.saveEverything();
        while(WDL.chunkSaver.isSaving())
        {
        	Sleep.sleep(500);
        }
        WDL.saving = false;
        WDL.onSaveComplete();
    }
}