// SWITCH.java, created Wed Aug 26 20:45:24 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Quads;

import harpoon.ClassFile.*;
import harpoon.Temp.Temp;
import harpoon.Temp.TempMap;
import harpoon.Util.Util;

/**
 * <code>SWITCH</code> represents a switch construct.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: SWITCH.java,v 1.1.2.2 1998-12-09 22:02:39 cananian Exp $
 */
public class SWITCH extends SIGMA {
    /** The discriminant, compared against each value in <code>keys</code>.*/
    protected Temp index;
    /** Integer keys for switch cases. <p>
     *  <code>next(n)</code> is the jump target corresponding to
     *  <code>keys[n]</code> for <code>0 <= n < keys.length</code>. <p>
     *  <code>next(keys.length)</code> is the default target. */
    protected int keys[];

    /** Creates a <code>SWITCH</code> operation. <p>
     *  <code>next[n]</code> is the jump target corresponding to
     *  <code>keys[n]</code> for <code>0 <= n < keys.length</code>. <p>
     *  <code>next[keys.length]</code> is the default target.
     * @param index
     *        the discriminant.
     * @param keys
     *        integer keys for switch cases.
     * @param dst
     *        sigma function left-hand sides.
     * @param src
     *        sigma function arguments.
     */
    public SWITCH(HCodeElement source,
		  Temp index, int keys[],
		  Temp dst[][], Temp src[]) {
	super(source, dst, src, keys.length+1 /*multiple targets*/);
	this.index = index;
	this.keys = keys;
	// VERIFY legality of SWITCH.
	Util.assert(index!=null && keys!=null);
	Util.assert(keys.length+1==arity());
    }
    /** Creates a switch with arity defined by the keys array. */
    public SWITCH(HCodeElement source, Temp index, int keys[], Temp src[]) {
	this(source, index, keys, new Temp[src.length][keys.length+1], src);
    }
    /** Returns the <code>Temp</code> holding the discriminant. */
    public Temp index() { return index; }
    /** Returns the array of integer keys for the switch cases. */
    public int[] keys() { return (int[]) keys.clone(); }
    /** Returns a given element in the <code>keys</code> array. */
    public int keys(int i) { return keys[i]; }
    /** Returns the length of the <code>keys</code> array. */
    public int keysLength() { return keys.length; }

    /** Returns the <code>Temp</code> used by this quad.
     * @return the <code>index</code> field. */
    public Temp[] use() { 
	Temp[] u = super.use();
	Temp[] r = new Temp[u.length+1];
	System.arraycopy(u, 0, r, 0, u.length);
	// add 'index' to end of use array.
	r[u.length] = index;
	return r;
    }

    /** Rename all used variables in this Quad according to a mapping. */
    public void renameUses(TempMap tm) {
	super.renameUses(tm);
	index = tm.tempMap(index);
    }
    /** Rename all defined variables in this Quad according to a mapping. */
    public void renameDefs(TempMap tm) {
	super.renameDefs(tm);
    }

    /** Properly clone <code>keys[]</code> array. */
    public Object clone() {
	SWITCH q = (SWITCH) super.clone();
	q.keys = (int[]) keys.clone();
	return q;
    }

    public void visit(QuadVisitor v) { v.visit(this); }

    /** Returns human-readable representation of this quad. */
    public String toString() {
	StringBuffer sb = new StringBuffer("SWITCH "+index+": ");
	for (int i=0; i<keys.length; i++)
	    sb.append("case "+keys[i]+" => "+next(i).getID()+"; ");
	sb.append("default => "+next(keys.length).getID());
	sb.append(" / "); sb.append(super.toString());
	return sb.toString();
    }
}
