// AllocationNumbering.java, created Wed Nov  8 19:06:08 2000 by cananian
// Copyright (C) 2000 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.PointerAnalysis;

import harpoon.Analysis.*;
import harpoon.ClassFile.*;
import harpoon.IR.Quads.*;

import java.util.*;
/**
 * <code>AllocationNumbering</code>
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: AllocationNumbering.java,v 1.1.2.1 2000-11-09 01:15:39 cananian Exp $
 */
public class AllocationNumbering implements java.io.Serializable {
    private final CachingCodeFactory hcf;
    private final Map alloc2int = new HashMap();
    
    /** Creates a <code>AllocationNumbering</code>. */
    public AllocationNumbering(HCodeFactory hcf, ClassHierarchy ch) {
        this.hcf = new CachingCodeFactory(hcf, true);
	int n = 0;
	for (Iterator it = ch.callableMethods().iterator(); it.hasNext(); ) {
	    HMethod hm = (HMethod) it.next();
	    n = number(hcf.convert(hm), n);
	}
    }
    /** Return the (caching) code factory this numbering was created on. */
    public HCodeFactory codeFactory() { return hcf; }
    
    /** Return an integer identifying this allocation site. */
    public int allocID(Quad q) {
	if (!alloc2int.containsKey(q)) throw new Error("Quad unknown: "+q);
	return ((Integer) alloc2int.get(q)).intValue();
    }

    /* hard part: the numbering */
    private int number(HCode hc, int n) {
	if (hc!=null)
	    for (Iterator it=hc.getElementsI(); it.hasNext(); ) {
		Quad q = (Quad) it.next();
		if (q instanceof ANEW || q instanceof NEW)
		    alloc2int.put(q, new Integer(n++));
	    }
	return n;
    }
}
