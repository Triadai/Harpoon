// InMerge.java, created by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Bytecode;

import harpoon.Util.Util;
/**
 * <code>InMerge</code> is used to represent a node where
 * multiple control flows merge.  These are typically branch targets.
 * An <code>InMerge</code> will have exactly one successor, and
 * multiple predecessors.
 *
 * @author  C. Scott Ananian
 * @version $Id: InMerge.java,v 1.2.4.1 1998-12-09 00:33:23 cananian Exp $
 * @see Instr
 */
public class InMerge extends Instr {
  int arity;

  /** Creates a new <code>InMerge</code>.
   * @param arity the number of predecessors this node will have.
   */
  public InMerge(String sourcefile, int linenumber, int arity) {
    super(sourcefile, linenumber);
    this.arity = arity;
  }

  // Provide run-time checks on arity.
  /** @see Instr#addPrev */
  void addPrev(Instr prev) {
    if (this.prev.size()>=arity) throw new Error("Exceeding arity of MERGE.");
    super.addPrev(prev);
  }
  /** @see Instr#addNext */
  void addNext(Instr next) {
    if (this.next.size()>=1) 
      throw new Error("MERGE should have only one successor.");
    super.addNext(next);
  }

  /** Return the number of predecessors of this node.
   *  Exactly equivalent to <code>prev().length</code> but slightly
   *  more efficient.
   */
  public int arity() {
      /* arity may be larger than this.prev.size() because of inputs
       * from try handlers. */
      //Util.assert(arity==this.prev.size());
      return arity;
  }

  /** Return the java bytecode of this instruction.
   *  This function is required by the superclass, but not relevant
   *  to this function.  It returns <code>Op.NOP</code>.
   */
  public byte getOpcode() { return Op.NOP; }


  /** Return human-readable representation. */
  public String toString() { return "MERGE/"+arity; }
}
