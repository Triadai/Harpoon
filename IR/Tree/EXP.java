// EXP.java, created Wed Jan 13 21:14:57 1999 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.ClassFile.HCodeElement;
import harpoon.Temp.TempMap;
import harpoon.Util.Util;

/**
 * <code>EXP</code> objects evaluate an expression (for side-effects) and then
 * throw away the result.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>, based on
 *          <i>Modern Compiler Implementation in Java</i> by Andrew Appel.
 * @version $Id: EXP.java,v 1.1.2.19 2000-02-15 17:19:04 cananian Exp $
 */
public class EXP extends Stm {
    /** Constructor. */
    public EXP(TreeFactory tf, HCodeElement source, 
	       Exp exp) {
	super(tf, source, 1);
	Util.assert(exp!=null);
	this.setExp(exp);
	Util.assert(tf == exp.tf, "Dest and Src must have same tree factory");
	
	// FSK: debugging hack
	// this.accept(TreeVerifyingVisitor.norepeats());
    }

    /** Returns the expression to evaluate. */
    public Exp getExp() { return (Exp) getChild(0); }

    /** Sets the expression to evaluate. */
    public void setExp(Exp exp) { setChild(0, exp); }

    public int kind() { return TreeKind.EXP; }

    public Stm build(TreeFactory tf, ExpList kids) {
	Util.assert(kids!=null && kids.tail==null);
	Util.assert(tf == kids.head.tf);
	return new EXP(tf, this, kids.head);
    }
    /** Accept a visitor */
    public void accept(TreeVisitor v) { v.visit(this); }

    public Tree rename(TreeFactory tf, TempMap tm, CloneCallback cb) {
        return cb.callback(this, new EXP(tf, this, (Exp)getExp().rename(tf, tm, cb)), tm);
    }

    public String toString() {
        return "EXP(#" + getExp().getID() + ")";
    }
}

