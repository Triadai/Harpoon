// CheckOracle.java, created Sun Nov 12 01:19:11 2000 by cananian
// Copyright (C) 2000 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.LowQuad.Transactions;

import harpoon.ClassFile.*;
import harpoon.Temp.Temp;

import java.util.*;
/**
 * A <code>CheckOracle</code> helps the SyncTransformer place
 * field and object version lookups and checks.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: CheckOracle.java,v 1.1.2.1 2000-11-14 19:37:33 cananian Exp $
 */
abstract class CheckOracle {
    
    /** Returns <code>Set</code> of <code>Temp</code>s for which read-only
     *  versions should be looked up just before <code>hce</code> is
     *  executed. */
    public abstract Set createReadVersions(HCodeElement hce);
    /** Returns <code>Set</code> of <code>Temp</code>s for which writable
     *  versions should be created just before <code>hce</code> is executed. */
    public abstract Set createWriteVersions(HCodeElement hce);
    /** Returns a <code>Set</code> of <code>RefAndField</code> tuples
     *  which should be checked just before <code>hce</code> is
     *  executed. */
    public abstract Set checkField(HCodeElement hce);
    /** Returns a <code>Set</code> of <code>RefAndIndexAndType</code>
     *  typles which indicate indexed array elements which should be
     *  checked just before <code>hce</code> is executed.  */
    public abstract Set checkArrayElement(HCodeElement hce);

    class RefAndField {
	public final Temp objref;
	public final HField field;
	RefAndField(Temp objref, HField field) {
	    this.objref = objref; this.field = field;
	}
    }
    class RefAndIndexAndType {
	public final Temp objref;
	public final Temp index;
	public final HClass type;
	RefAndIndexAndType(Temp objref, Temp index, HClass type) {
	    this.objref = objref; this.index = index; this.type = type;
	}
    }
}
