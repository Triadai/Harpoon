package harpoon.Analysis.ContBuilder;


// adapter class. to be eliminated when we switch to optimism
public class ObjectDoneContinuation extends ObjectContinuation implements VoidResultContinuation {

    Object result;
    Throwable throwable;
    boolean isResume;

    public ObjectDoneContinuation(Object result) {
	this.result= result;
	this.isResume=true;
	Scheduler.addReady(this);
    }

    public ObjectDoneContinuation(Throwable throwable) {
	this.throwable=throwable;
	this.isResume=false;
	Scheduler.addReady(this);
    }

    public void resume() {
	if (isResume)
	    next.resume(result);
	else
	    next.exception(throwable);
    }

    static public ObjectContinuation pesimistic(ObjectContinuation c)
    {
	return c!=null? c : new ObjectDoneContinuation(ObjectContinuation.result);
    }


    public void exception(Throwable t) {
    }
    private Continuation link;
    public final void setLink(Continuation c) { link= c; }
    public final Continuation getLink() { return link; }
}
