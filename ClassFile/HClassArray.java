// HClassArray.java, created Wed Dec 29 22:24:55 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

import harpoon.Util.Util;

import java.lang.reflect.Modifier;
/**
 * <code>HClassArray</code> is a simple <code>HClass</code> implementation
 * representing array types.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: HClassArray.java,v 1.1.4.3 2000-10-20 22:48:20 cananian Exp $
 */
class HClassArray extends HClassImpl {
  HClass baseType;
  int dims;
  HField lengthField;
  HMethod cloneMethod;

  HClassArray(Linker linker, HClass baseType, int dims) {
    super(linker);
    this.baseType = baseType; this.dims = dims;
    this.lengthField = new HArrayField(this, "length", HClass.Int,
				       Modifier.PUBLIC | Modifier.FINAL);
    this.cloneMethod = new HArrayMethod(this, "clone",
					Modifier.PUBLIC | Modifier.NATIVE,
					HCobject, new HClass[0], new String[0],
					new HClass[0], false);
  }
  public HClass getComponentType() {
    return getLinker().forDescriptor(getDescriptor().substring(1));
  }
  public String getName() {
    // handle arrays.
    return getDescriptor(); // this is how sun's implementation works.
  }
  public String getDescriptor() {
    return Util.repeatString("[", dims) + baseType.getDescriptor();
  }
  public HField [] getDeclaredFields () { 
    return new HField[] { lengthField };
  }
  public HMethod[] getDeclaredMethods() {
    return new HMethod[] { cloneMethod };
  }
  public int getModifiers() {
    // this is what java.lang.Class returns.
    return Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.FINAL;
  }
  public HClass getSuperclass() { return HCobject; }
  public HClass[] getInterfaces() {
    // see http://java.sun.com/docs/books/jls/clarify.html
    return new HClass[] { HCcloneable, HCserializable };
  }
  public boolean isArray() { return true; }

  // cache class constants.
  private final HClass HCobject   =getLinker().forName("java.lang.Object");
  private final HClass HCcloneable=getLinker().forName("java.lang.Cloneable");
  private final HClass HCserializable=getLinker().forName("java.io.Serializable");
}

// set emacs indentation style.
// Local Variables:
// c-basic-offset:2
// End:
