// HPointer.java, created Thu Dec 10 23:41:19 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

import harpoon.Util.ArrayFactory;

/**
 * The <code>HPointer</code> interface allows us to resolve
 * pointers to <code>HClass</code>es transparently (and allows us
 * to demand-load class files, instead of doing them all at once).
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: HPointer.java,v 1.1.2.4 2000-01-13 23:47:47 cananian Exp $
 */
abstract class HPointer  {
    /** Returns a genuine HClass from the (possible) pointer. */
    abstract HClass actual();
    abstract String getName();
    abstract String getDescriptor();
    /** 
     * Returns a hashcode value for this HClass.
     * The hashcode is identical to the hashcode for the class descriptor
     * string. 
     */
    public int hashCode() { return getDescriptor().hashCode(); }

    public static final ArrayFactory arrayFactory = 
	new ArrayFactory() {
	    public Object[] newArray(int len) { return new HPointer[len]; }
	};
}
