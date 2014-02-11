package xurei.wdl;

public class WDLSaveProgressReporter implements Runnable
{
    public void run()
    {
        while (WDL.saving)
        {
            WDL.chatMsg("Saving...");

            try 
            {
                Thread.sleep(10000L);
            } 
            catch (InterruptedException e) 
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void start()
    {
        Thread t = new Thread(this);
        t.start();
    }
}
