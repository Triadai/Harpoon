// NullNativeMethod.java, created Sat Aug  7 01:56:53 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Interpret.Tree;

import harpoon.ClassFile.HMethod;
/**
 * <code>NullNativeMethod</code> is a native method which does nothing.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: NullNativeMethod.java,v 1.1.2.1 2000-01-12 00:44:32 cananian Exp $
 */
class NullNativeMethod extends NativeMethod {
    private final HMethod hm;
    /** Creates a <code>NullNativeMethod</code> for the specified
     *  <code>HMethod</code>. */
    NullNativeMethod(HMethod hm) { this.hm = hm; }
    HMethod getMethod() { return hm; }
    Object invoke(StaticState ss, Object[] params) { return null; }
}
