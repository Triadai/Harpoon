// CJMP.java, created Wed Aug  5 07:07:32 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Quads;

import harpoon.ClassFile.*;
import harpoon.Temp.Temp;
import harpoon.Temp.TempMap;
import harpoon.Util.Util;

/**
 * <code>CJMP</code> represents conditional branches.<p>
 * <code>next[0]</code> is if-false, which is taken if 
 *                         the operand is equal to zero.
 * <code>next[1]</code> is if-true branch, taken when 
 *                         the operand is not equal to zero.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: CJMP.java,v 1.1.2.5 1998-12-20 07:11:59 cananian Exp $
 */
public class CJMP extends SIGMA {
    protected Temp test;

    /** Creates a <code>CJMP</code> representing a conditional branch.
     * @param test
     *        the <code>Temp</code> tested by this branch.
     * @param src
     *        the source <code>Temp</code>s for the underlying 
     *        <code>SIGMA</code>. 
     * @param dst
     *        the destination <code>Temp</code>s for the underlying
     *        <code>SIGMA</code>.
     */
    public CJMP(QuadFactory qf, HCodeElement source,
		Temp test, Temp dst[][], Temp src[]) {
        super(qf, source, dst, src, 2 /* two branch targets */);
	this.test = test;
	Util.assert(test!=null);
    }
    /** Creates a <code>CJMP</code> representing a conditional branch.
     *  Abbreviated form of the constructor uses an appropriately-sized
     *  array of <code>null</code> values for the <code>dst</code> field.
     */
    public CJMP(QuadFactory qf, HCodeElement source, Temp test, Temp src[]) {
	this(qf, source, test, new Temp[src.length][2], src);
    }
    // ACCESSOR FUNCTIONS:
    /** Returns the <code>Temp</code> tested by this <code>CJMP</code>. */
    public Temp test() { return test; }

    /** Swaps if-true and if-false targets, inverting the sense of the
     *  test.
     *  @deprecated Violates immutability of quads. */
    public void invert() {
	Edge iftrue = nextEdge(0);
	Edge iffalse= nextEdge(1);

	Quad.addEdge(this, 0, (Quad)iffalse.to(), iffalse.which_pred());
	Quad.addEdge(this, 1, (Quad)iftrue.to(), iftrue.which_pred());

	for (int i=0; i<src.length; i++) {
	    Temp Ttrue = dst[i][0];
	    Temp Tfalse= dst[i][1];
	    dst[i][0] = Tfalse;
	    dst[i][1] = Ttrue;
	}
    }
    /** Returns all the Temps used by this Quad.
     * @return the <code>test</code> field.
     */
    public Temp[] use() { 
	Temp[] u = super.use();
	Temp[] r = new Temp[u.length+1];
	System.arraycopy(u, 0, r, 0, u.length);
	// add 'test' to end of use array.
	r[u.length] = test;
	return r;
    }

    public int kind() { return QuadKind.CJMP; }

    public Quad rename(QuadFactory qqf, TempMap tm) {
	return new CJMP(qqf, this, map(tm, test), map(tm, dst), map(tm, src));
    }
    /** Rename all used variables in this Quad according to a mapping. */
    void renameUses(TempMap tm) {
	super.renameUses(tm);
	test = tm.tempMap(test);
    }
    /** Rename all defined variables in this Quad according to a mapping. */
    void renameDefs(TempMap tm) {
	super.renameDefs(tm);
    }

    public void visit(QuadVisitor v) { v.visit(this); }

    /** Returns human-readable representation. */
    public String toString() {
	return "CJMP: if " + test + 
	    " / " + super.toString();
    }
}
