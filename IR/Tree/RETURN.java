// RETURN.java, created Thu Feb 18 16:59:53 1999 by duncan
// Copyright (C) 1998 Duncan Bryce <duncan@lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.ClassFile.HCodeElement;
import harpoon.Temp.TempMap;
import harpoon.Util.Util;

/**
 * <code>RETURN</code> objects are used to represent a return from 
 * a method body.
 *
 * @author   Duncan Bryce <duncan@lcs.mit.edu>, based on
 *          <i>Modern Compiler Implementation in Java</i> by Andrew Appel.
 * @version  $Id: RETURN.java,v 1.1.2.16 2001-06-17 23:07:53 cananian Exp $
 */
public class RETURN extends Stm implements Typed {
    /** Constructor.
     *  @param retval  the value to return.  Never null.
     */
    public RETURN(TreeFactory tf, HCodeElement source, 
		  Exp retval) {
	super(tf, source, 1);
	Util.assert(retval!=null);
	this.setRetval(retval);
	Util.assert(tf == retval.tf, "This and Retval must have same tree factory");
    }		

    /** Returns the value to return. */
    public Exp getRetval() { return (Exp) getChild(0); }
    /** Sets the value to return. */
    public void setRetval(Exp retval) { setChild(0, retval); }

    public int kind() { return TreeKind.RETURN; }

    public Stm build(TreeFactory tf, ExpList kids) {
	Util.assert(kids!=null && kids.tail==null);
	Util.assert(tf == kids.head.tf);
	return new RETURN(tf, this, kids.head);
    }

    /** Accept a visitor */
    public void accept(TreeVisitor v) { v.visit(this); }

    public Tree rename(TreeFactory tf, TempMap tm, CloneCallback cb) {
	return cb.callback(this, new RETURN(tf, this, (Exp)getRetval().rename(tf, tm, cb)), tm);
    }

    /** @return the type of the return value expression */
    public int type() { return getRetval().type(); }
    public boolean isDoubleWord() { return getRetval().isDoubleWord(); }
    public boolean isFloatingPoint() { return getRetval().isFloatingPoint(); }
    
    public String toString() {
	return "RETURN(#"+getRetval().getID()+")";
    }

}