// HField.java, created Fri Jul 31  9:33:47 1998 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

import harpoon.Util.ArrayFactory;

import java.lang.reflect.Modifier;

/**
 * A <code>HField</code> provides information about a single field of a class
 * or an interface.  The reflected field may be a class (static) field or
 * an instance field.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: HField.java,v 1.16 2002-02-25 21:03:03 cananian Exp $
 * @see HMember
 * @see HClass
 */
public interface HField extends HMember {
  /** 
   * Returns the <code>HClass</code> object representing the class or
   * interface that declares the field represented by this 
   * <code>HField</code> object. 
   */
  public HClass getDeclaringClass();

  /**
   * Returns the name of the field represented by this 
   * <code>HField</code> object.
   */
  public String getName();

  /**
   * Returns the Java language modifiers for the field represented by this
   * <code>HField</code> object, as an integer.  The <code>Modifier</code>
   * class should be used to decode the modifiers.
   * @see java.lang.reflect.Modifier
   */
  public int getModifiers();

  /**
   * Returns an <code>HClass</code> object that identifies the declared
   * type for the field represented by this <code>HField</code> object.
   */
  public HClass getType();

  /**
   * Return the type descriptor for this <code>HField</code> object.
   */
  public String getDescriptor();

  /**
   * Returns the constant value of this <code>HField</code>, if
   * it is a constant field.
   * @return the wrapped value, or <code>null</code> if 
   *         <code>!isConstant()</code>.
   * @see HClass#getWrapper
   */
  public Object getConstant();

  /**
   * Determines whether this <code>HField</code> represents a constant
   * field.
   */
  public boolean isConstant();

  /**
   * Determines whether this <code>HField</code> is synthetic.
   */
  public boolean isSynthetic();

  /** Determines whether this is a static field. */
  public boolean isStatic();

  /** Returns a mutator for this <code>HField</code>, or <code>null</code>
   *  if the object is immutable. */
  public HFieldMutator getMutator();

  /** 
   * Compares this <code>HField</code> against the specified object.
   * Returns <code>true</code> if the objects are the same.  Two
   * <code>HFields</code> are the same if they were declared by the same
   * class and have the same name and type.
   */
  public boolean equals(Object object);

  /**
   * Return a string describing this <code>HField</code>.  The format
   * is the access modifiers for the field, if any, followed by the
   * field type, followed by a space, followed by the fully-qualified
   * name of the class declaring the field, followed by a period,
   * followed by the name of the field.  For example:<p>
   * <DL>
   * <DD><CODE>public static final int java.lang.Thread.MIN_PRIORITY</CODE>
   * <DD><CODE>private int java.io.FileDescriptor.fd</CODE>
   * </DL><p>
   * The modifiers are placed in canonical order as specified by
   * "The Java Language Specification."  This is
   * <code>public</code>, <code>protected</code>, or <code>private</code>
   * first, and then other modifiers in the following order:
   * <code>static</code>, <code>final</code>, <code>transient</code>,
   * <code>volatile</code>.
   */
  public String toString();

  /** Array factory: returns new <code>HField[]</code>. */
  public static final ArrayFactory arrayFactory =
    Factories.hfieldArrayFactory;
}
// set emacs indentation style.
// Local Variables:
// c-basic-offset:2
// End: