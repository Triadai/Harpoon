// HClassPrimitive.java, created Wed Dec 29 22:22:49 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

/**
 * <code>HClassPrimitive</code> is a simple <code>HClass</code>
 * implementation to represent primitive types.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: HClassPrimitive.java,v 1.1.4.2 2000-01-15 00:49:06 cananian Exp $
 */
class HClassPrimitive extends HClassImpl {
  final String name, descriptor;
  HClassPrimitive(final String name, final String descriptor) {
    super(Loader.systemLinker);
    this.name = name; this.descriptor = descriptor;
  }
  public String getName() { return this.name; }
  public String getDescriptor() { return this.descriptor; }

  public HField[]  getDeclaredFields () { return new HField [0]; }
  public HMethod[] getDeclaredMethods() { return new HMethod[0]; }
  public int getModifiers() { 
    throw new Error("No modifiers for primitive types.");
  }
  public HClass getSuperclass() { return null; }
  public HClass[] getInterfaces() { return new HClass[0]; }
  public boolean isPrimitive() { return true; }
}
// set emacs indentation style.
// Local Variables:
// c-basic-offset:2
// End:
