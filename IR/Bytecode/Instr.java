// Instr.java, created by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Bytecode;

import harpoon.ClassFile.HCodeEdge;
import harpoon.ClassFile.HCodeElement;
import harpoon.Util.ArrayFactory;

import java.util.Vector;
/**
 * <code>Bytecode.Instr</code> is the base type for the specific
 * bytecode instruction classes.  It provides standard methods
 * for accessing the opcode of a specific instruction and for
 * determining which instructions may preceed or follow it.
 * <p>As with all <code>HCodeElement</code>s, <code>Instr</code>s are
 * traceable to an original source file and line number, and have
 * a unique numeric identifier.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Instr.java,v 1.3.2.4 1999-01-22 23:05:47 cananian Exp $
 * @see InGen
 * @see InCti
 * @see InMerge
 * @see InSwitch
 * @see Code
 */
public abstract class Instr 
  implements HCodeElement, harpoon.IR.Properties.Edges {
  /*final*/ String sourcefile;
  /*final*/ int linenumber;
  /*final*/ int id;
  /** Constructor. */
  protected Instr(String sourcefile, int linenumber) {
    this.sourcefile = sourcefile;
    this.linenumber = linenumber;
    synchronized(lock) {
      this.id = next_id++;
    }
  }
  static int next_id = 0;
  static final Object lock = new Object();

  /** Returns the original source file name that this bytecode instruction 
   *  is derived from. */
  public String getSourceFile() { return sourcefile; }
  /** Returns the line in the original source file that this bytecode 
   *  instruction can be traced to. */
  public int getLineNumber() { return linenumber; }
  /** Returns a unique numeric identifier for this element. */
  public int getID() { return id; }
  /** Returns the java bytecode of this instruction. */
  public abstract byte getOpcode();

  /** Array Factory: makes <code>Instr[]</code>s. */
  public static final ArrayFactory arrayFactory =
    new ArrayFactory() {
      public Object[] newArray(int len) { return new Instr[len]; }
    };

  /** Return a list of all the <code>Instr</code>s that can precede
   *  this one. */
  public Instr[] prev() {
    Instr[] p = new Instr[prev.size()]; prev.copyInto(p); return p;
  }
  /** Return a list of all the possible <code>Instr</code>s that may
   *  succeed this one. */
  public Instr[] next() {
    Instr[] n = new Instr[next.size()]; next.copyInto(n); return n;
  }
  /** Return the specified successor of this <code>Instr</code>. */
  public Instr next(int i) { return (Instr) next.elementAt(i); }
  /** Return the specified predecessor of this <code>Instr</code>. */
  public Instr prev(int i) { return (Instr) prev.elementAt(i); }

  /** Add a predecessor to this <code>Instr</code>. */
  void addPrev(Instr prev) { this.prev.addElement(prev); }
  /** Add a successor to this <code>Instr</code>. */
  void addNext(Instr next) { this.next.addElement(next); }
  /** Remove a predecessor from this <code>Instr</code>. */
  void removePrev(Instr prev) { this.prev.removeElement(prev); }
  /** Remove a successor from this <code>Instr</code>. */
  void removeNext(Instr next) { this.next.removeElement(next); }

  /** Internal predecessor list. */
  final Vector prev = new Vector(2);
  /** Internal successor list. */
  final Vector next = new Vector(2);

  // Edges interface:
  public HCodeEdge newEdge(final Instr from, final Instr to) {
    return new HCodeEdge() {
      public HCodeElement from() { return from; }
      public HCodeElement to() { return to; }
      public boolean equals(Object o) { 
	return (o instanceof HCodeEdge &&
		((HCodeEdge)o).from() == from &&
		((HCodeEdge)o).to() == to);
      }
      public int hashCode() { return from.hashCode() ^ to.hashCode(); }
    };
  }
  public HCodeEdge[] succ() {
    HCodeEdge[] r = new HCodeEdge[next.size()];
    for (int i=0; i<r.length; i++)
      r[i] = newEdge(this, (Instr) next.elementAt(i));
    return r;
  }
  public HCodeEdge[] pred() {
    HCodeEdge[] r = new HCodeEdge[prev.size()];
    for (int i=0; i<r.length; i++)
      r[i] = newEdge((Instr)prev.elementAt(i), this);
    return r;
  }
  public HCodeEdge[] edges() {
    HCodeEdge[] n = succ();
    HCodeEdge[] p = pred();
    HCodeEdge[] r = new HCodeEdge[n.length + p.length];
    System.arraycopy(n, 0, r, 0, n.length);
    System.arraycopy(p, 0, r, n.length, p.length);
    return r;
  }
}
