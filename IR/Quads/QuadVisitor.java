// QuadVisitor.java, created Fri Sep 11 12:59:44 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Quads;

import harpoon.ClassFile.*;
/**
 * <code>QuadVisitor</code> is a Design Pattern, courtesy of Martin.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: QuadVisitor.java,v 1.1.2.2 1998-12-09 22:02:36 cananian Exp $
 */

public abstract class QuadVisitor  {
    protected QuadVisitor() { }

    /** Visit a quad q. */
    public abstract void visit(Quad q);
    public void visit(AGET q)		{ visit((Quad)q); }
    public void visit(ALENGTH q)	{ visit((Quad)q); }
    public void visit(ANEW q)		{ visit((Quad)q); }
    public void visit(ASET q)		{ visit((Quad)q); }
    public void visit(CALL q)		{ visit((Quad)q); }
    public void visit(CJMP q)		{ visit((SIGMA)q); }
    public void visit(COMPONENTOF q)	{ visit((Quad)q); }
    public void visit(CONST q)		{ visit((Quad)q); }
    public void visit(DEBUG q)          { visit((Quad)q); }
    public void visit(FOOTER q)		{ visit((Quad)q); }
    public void visit(GET q)		{ visit((Quad)q); }
    public void visit(HEADER q)		{ visit((Quad)q); }
    public void visit(INSTANCEOF q)	{ visit((Quad)q); }
    public void visit(SIGMA q)		{ visit((Quad)q); }
    public void visit(MONITORENTER q)	{ visit((Quad)q); }
    public void visit(MONITOREXIT q)	{ visit((Quad)q); }
    public void visit(MOVE q)		{ visit((Quad)q); }
    public void visit(NEW q)		{ visit((Quad)q); }
    public void visit(NOP q)		{ visit((Quad)q); }
    public void visit(OPER q)		{ visit((Quad)q); }
    public void visit(PHI q)		{ visit((Quad)q); }
    public void visit(RETURN q)		{ visit((Quad)q); }
    public void visit(SET q)		{ visit((Quad)q); }
    public void visit(SWITCH q)		{ visit((SIGMA)q); }
    public void visit(THROW q)		{ visit((Quad)q); }
}
