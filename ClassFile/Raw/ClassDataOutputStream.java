// ClassDataOutputStream.java, created by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile.Raw;

/**
 * A specialized output stream writer for java bytecode files.
 *
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: ClassDataOutputStream.java,v 1.7 1998-10-11 03:01:12 cananian Exp $
 * @see ClassDataInputStream
 */
public class ClassDataOutputStream extends java.io.DataOutputStream {

  /** Constructs a <code>ClassDataOutputStream</code> from a standard
   *  <code>OutputStream</code>. */
  public ClassDataOutputStream(java.io.OutputStream out) {
    super(out);
  }

  /** Write an unsigned one-byte quantity, high byte first. */
  public final void write_u1(int v) throws java.io.IOException
  { writeByte(v); }
  /** Write an unsigned two-byte quantity, high byte first. */
  public final void write_u2(int v) throws java.io.IOException
  { writeShort(v); }
  /** Write an unsigned four-byte quantity, high byte first. */
  public final void write_u4(long v) throws java.io.IOException
  { writeShort((int)((v>>16)&0xFFFF)); writeShort((int)(v&0xFFFF)); }
}
