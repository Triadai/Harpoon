// ContCode.java, created Wed Nov  3 21:43:30 1999 by kkz
// Copyright (C) 1999 Karen K. Zee <kkzee@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.EventDriven;

import harpoon.Analysis.Quads.Unreachable;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HField;
import harpoon.ClassFile.HMethod;
import harpoon.IR.Quads.Quad;
import harpoon.IR.Quads.QuadFactory;
import harpoon.Temp.Temp;
import harpoon.Temp.TempFactory;
import harpoon.Util.Util;

import java.util.Iterator;
import java.util.Map;

/**
 * <code>ContCode</code> builds the code for a <code>Continuation</code>
 * using <code>quad-no-ssa</code> <code>HCode</code>.
 * 
 * @author Karen K. Zee <kkzee@alum.mit.edu>
 * @version $Id: ContCode.java,v 1.1.2.2 2000-02-08 08:40:10 bdemsky Exp $
 */
public class ContCode extends harpoon.IR.Quads.QuadSSI {

    /** Creates a <code>ContCode</code> for an <code>HMethod</code> using
     *  the <code>HCode</code> from which we want to build the continuation
     *  and the <code>CALL</code> at which we want the continuation built.
     *  The <code>HCode</code> must be <code>quad-no-ssa</code>.
     *
     */
    public ContCode(HMethod parent) {
        super(parent, null);
    }

    /**
     * Return the name of this code view.
     * @return the name of the <code>parent</code>'s code view.
     */
    public String getName() {
	return harpoon.IR.Quads.QuadSSI.codename;
    }

    public void quadSet(Quad q) {
        this.quads=q;
    }

    public QuadFactory getFactory() {
        return qf;
    }
}
