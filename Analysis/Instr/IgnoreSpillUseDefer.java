// IgnoreSpillUDr.java, created Fri Jun 30 19:14:06 2000 by pnkfelix
// Copyright (C) 2000 Felix S. Klock <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.Instr;

import harpoon.IR.Properties.UseDef;
import harpoon.IR.Properties.UseDefer;
import harpoon.ClassFile.HCodeElement;
import java.util.Collection;
import java.util.Collections;

/**
 * <code>IgnoreSpillUseDefer</code>
 * 
 * @author  Felix S. Klock <pnkfelix@mit.edu>
 * @version $Id: IgnoreSpillUseDefer.java,v 1.1.2.6 2000-07-11 00:16:26 pnkfelix Exp $
 */
public class IgnoreSpillUseDefer extends UseDefer {

    public static IgnoreSpillUseDefer USEDEFER = 
	new IgnoreSpillUseDefer();
    
    /** Creates a <code>IgnoreSpillUDr</code>. */
    public IgnoreSpillUseDefer() {
        
    }
    
    public Collection useC(HCodeElement hce) {
	if (hce instanceof RegAlloc.SpillStore)
	    return Collections.EMPTY_SET;
	else 
	    return ((UseDef)hce).useC();
    }

    public Collection defC(HCodeElement hce) {
	if (hce instanceof RegAlloc.SpillLoad)
	    return Collections.EMPTY_SET;
	else
	    return ((UseDef)hce).defC();
    }
}
