// LowQuadVisitor.java, created Tue Jan 19 21:15:19 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.LowQuad;

/**
 * <code>LowQuadVisitor</code> is another design pattern.  We live to
 * make Martin happy.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: LowQuadVisitor.java,v 1.1.2.2 1999-01-21 05:19:14 cananian Exp $
 */
public abstract class LowQuadVisitor extends harpoon.IR.Quads.QuadVisitor {
    protected LowQuadVisitor() { }

    // DISALLOW certain Quads in LowQuad form.
    /** <code>AGET</code> is disallowed in <code>LowQuad</code> form.
     *  This method throws an <code>Error</code>. */
    public final void visit(harpoon.IR.Quads.AGET q)    { error(q); }
    /** <code>ASET</code> is disallowed in <code>LowQuad</code> form.
     *  This method throws an <code>Error</code>. */
    public final void visit(harpoon.IR.Quads.ASET q)    { error(q); }
    /** <code>GET</code> is disallowed in <code>LowQuad</code> form.
     *  This method throws an <code>Error</code>. */
    public final void visit(harpoon.IR.Quads.GET q)     { error(q); }
    /** <code>HANDLER</code> is disallowed in <code>LowQuad</code> form.
     *  This method throws an <code>Error</code>. */
    public final void visit(harpoon.IR.Quads.HANDLER q) { error(q); }
    /** <code>OPER</code> is disallowed in <code>LowQuad</code> form.
     *  This method throws an <code>Error</code>. */
    public final void visit(harpoon.IR.Quads.OPER q)    { error(q); }
    /** <code>SET</code> is disallowed in <code>LowQuad</code> form.
     *  This method throws an <code>Error</code>. */
    public final void visit(harpoon.IR.Quads.SET q)     { error(q); }
    // error function.
    private static final void error(harpoon.IR.Quads.Quad q) {
	throw new Error("Illegal LowQuad: "+q);
    }

    // Visitor functions for new LowQuads.
    public void visit(LowQuad q) { visit((harpoon.IR.Quads.Quad)q); }
    public void visit(PCALL q) { visit((LowQuad)q); }
    public void visit(PGET q)  { visit((LowQuad)q); }
    public void visit(POPER q) { visit((harpoon.IR.Quads.Quad)q); }
    public void visit(PPTR q)  { visit((LowQuad)q); }
    public void visit(PSET q)  { visit((LowQuad)q); }
}
