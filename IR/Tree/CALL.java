// CALL.java, created Wed Jan 13 21:14:57 1999 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.ClassFile.HCodeElement;
import harpoon.Temp.TempMap;
import harpoon.Util.Util;

import java.util.HashSet;
import java.util.Set;

/**
 * <code>CALL</code> objects are statements which stand for 
 * java method invocations, using our runtime's calling convention.
 * <p>
 * The <code>handler</code> expression is a
 * <code>Tree.NAME</code> specifying the label to which we should return
 * from this call if an exception occurs.  If the called method throws
 * an exception, the throwable object is placed in the <code>Temp</code>
 * specified by <code>retex</code> and a control tranfer to the
 * <code>Label</code> specified by <code>handler</code> occurs.
 * <p>
 * If there is no exception thrown by the callee, then the return
 * value is placed in the <code>Temp</code> specified by
 * <code>retval</code> and execution continues normally.  Note that
 * <code>retval</code> may be null if the called method has void return
 * type.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>, based on
 *          <i>Modern Compiler Implementation in Java</i> by Andrew Appel.
 * @version $Id: CALL.java,v 1.1.2.33 2000-02-15 17:19:04 cananian Exp $
 * @see harpoon.IR.Quads.CALL
 * @see INVOCATION
 * @see NATIVECALL
 */
public class CALL extends INVOCATION {
    /** Whether this invocation should be performed as a tail call. */
    public final boolean isTailCall;

    /** Create a <code>CALL</code> object.
     * @param retex Destination for any exception which the callee
     *  might throw.  Must be non-null.
     * @param handler Expression indicating the destination to which
     *  we should return if our caller throws an exception.
     */
    public CALL(TreeFactory tf, HCodeElement source,
		TEMP retval, TEMP retex, Exp func, ExpList args,
		NAME handler, boolean isTailCall) {
	super(tf, source, retval, func, args, 2); 
	Util.assert(retex != null && handler != null);
	Util.assert(retex.tf == tf);
	this.setRetex(retex); this.setHandler(handler);
	this.isTailCall = isTailCall;
	
	// FSK: debugging hack
	// this.accept(TreeVerifyingVisitor.norepeats());
    }

    /** Returns the destination expression for any exception which
     *  the callee might throw.  Guaranteed to be non-null. */
    public TEMP getRetex() { return (TEMP) getChild(0); }
    /** Returns an expression indicating the destination to which
     *  we should return if our caller throws an exception. */
    public NAME getHandler() { return (NAME) getChild(1); } 
  
    /** Sets the destination temp for any exception which the callee
     *  might throw.  Must be non-null. */
    public void setRetex(TEMP retex) {
	Util.assert(retex!=null);
	setChild(0, retex);
    }
    /** Sets the destination to which we should return
     *  if our caller throws an exception. */
    public void setHandler(NAME handler) { setChild(1, handler); }

    public boolean isNative() { return false; }

    public int kind() { return TreeKind.CALL; }

    public Stm build(TreeFactory tf, ExpList kids) {
	for (ExpList e = kids; e!=null; e=e.tail)
	    Util.assert(e.head == null || tf == e.head.tf);
	Util.assert(tf==this.tf, "cloning retval/retex/handler not yet impl.");

	return new CALL(tf, this, getRetval(), getRetex(),
			kids.head,            // func
			kids.tail,            // args
			getHandler(),         // handler
			isTailCall);
    }

    /** Accept a visitor */
    public void accept(TreeVisitor v) { v.visit(this); }

    public Tree rename(TreeFactory tf, TempMap tm, CloneCallback cb) {
        return cb.callback(this,
			   new CALL(tf, this, 
				    (TEMP)getRetval().rename(tf, tm, cb),
				    (TEMP)getRetex().rename(tf, tm, cb), 
				    (Exp)getFunc().rename(tf, tm, cb),
				    ExpList.rename(getArgs(), tf, tm, cb),
				    (NAME)getHandler().rename(tf, tm, cb),
				    isTailCall),
			   tm);
    }

    protected Set defSet() { 
	Set def = super.defSet();
	def.add(getRetex().temp);
	return def;
    }

    protected Set useSet() { 
	Set uses = super.useSet();
	return uses;
    }

    public String toString() {
        ExpList list;
        StringBuffer s = new StringBuffer();
        s.append("CALL(");
	if (this.getRetval()==null) { s.append("null"); } 
	else { s.append("#"+this.getRetval().getID()); } 
	s.append(", #"+this.getRetex().getID()+
                 ", #" + this.getFunc().getID() + ", {");
        list = this.getArgs();
        while (list != null) {
            s.append(" #"+list.head.getID());
            if (list.tail != null) {
                s.append(",");
            }
            list = list.tail;
        }
        s.append(" }");
	s.append(", #"+this.getHandler().getID());
	s.append(")");
	if (isTailCall) s.append(" [tail call]");
        return new String(s);
    }
}
