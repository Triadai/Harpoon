package harpoon.Analysis.ContBuilder;


// adapter class. to be eliminated when we switch to optimism
// OR if we decide we stay with pesimism
public final class ByteDoneContinuation extends ByteContinuation implements VoidResultContinuation {

    byte result;
    Throwable throwable;
    boolean isResume;

    public ByteDoneContinuation(byte result) {
	this.result= result;
	this.isResume=true;
	Scheduler.addReady(this);
    }

    public ByteDoneContinuation(Throwable throwable) {
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

    //BCD start
    public void exception(Throwable t) {
    }
	    
    private Continuation link;
    public final void setLink(Continuation c) { link= c; }
    public final Continuation getLink() { return link; }
    //BCD end
}


