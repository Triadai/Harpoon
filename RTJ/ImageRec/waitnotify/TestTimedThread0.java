import javax.realtime.AsyncEventHandler;
import javax.realtime.ImportanceParameters;
import javax.realtime.PeriodicParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.ReleaseParameters;
import javax.realtime.SchedulingParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.Timed;
import javax.realtime.AsynchronouslyInterruptedException;
import javax.realtime.Interruptible;
import javax.realtime.Clock;
import javax.realtime.AbsoluteTime;

class TestTimedThread0 extends TimedThread
{
    public int id;
    public AbsoluteTime start;
    protected AbsoluteTime beginCycle;
    protected AbsoluteTime endCycle;

    public TestTimedThread0(SchedulingParameters sp, RelativeTime reltime, int id)
    {
	super(sp, reltime);
	this.id = id;
    }

    protected int getid()
    {
	return id;
    }

    protected String getTime()
    {
	return (Clock.getRealtimeClock().getTime().subtract(start)).toString();
    }

    public void ProcessData() throws AsynchronouslyInterruptedException
    {
	beginCycle = Clock.getRealtimeClock().getTime();
	System.out.println(id + ": In logic - " + currentData.number0 + " - " + getTime());

	for(int i=0;i<300000;i++)
	{
	    currentData.number0 = (currentData.number0+3-4+2)*12/3/4; //add 1
	    if(currentData.number0%10000==0) yield();
	}

	endCycle = Clock.getRealtimeClock().getTime();
        System.out.println(id + ": Number: " + currentData.number0 + " - " + getTime() + " Time = " + endCycle.subtract(beginCycle));
    }

    public void onInterrupted()
    {
	endCycle = Clock.getRealtimeClock().getTime();
	System.out.println(id + ": Interrupted - " + getTime() + " Number = " + currentData.number0 + " Time = " + endCycle.subtract(beginCycle));
    }

    protected void Initialize()
    {
	currentData = new ImageData();
	currentData.number0 = currentData.number1 = currentData.number2 = 0;
    }
}