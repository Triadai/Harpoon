// Frame.java, created Fri Feb  5 05:48:12 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Backend.Generic;

import harpoon.Temp.Temp;
import harpoon.IR.Tree.Exp;
import harpoon.Backend.Maps.OffsetMap;
import harpoon.Temp.TempFactory;

/**
 * A <code>Frame</code> encapsulates the machine-dependent information
 * needed for compilation.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Frame.java,v 1.1.2.7 1999-02-17 07:15:11 andyb Exp $
 */
public interface Frame {

    /** Returns a <code>Tree.Exp</code> object which represents a pointer
     *  to a newly allocated block of memory, of the specified size.  
     *  Generates code to handle garbage collection, and OutOfMemory errors.
     */
    public Exp malloc(Exp size);
    
    /** Returns the appropriate offset map for this frame */
    public OffsetMap offsetMap();

    /** Returns <code>false</code> if pointers can be represented in
     *  32 bits, or <code>true</code> otherwise. */
    public boolean pointersAreLong();

    /** Returns a <code>Temp</code> to represent where return values
     *  for procedures will be stored. */
    public Temp RV();

    /** Returns a <code>Temp</code> to represent where exceptional return 
     *  values for procedures will be stored */
    public Temp RX();

    /** Returns a specially-named Temp to use for the frame pointer.
     */
    public Temp FP();

    /** Returns an array of <code>Temp</code>s which represent all
     *  the available registers on the machine. */
    public Temp[] registers();

    /** Returns the TempFactory used by this Frame */
    public TempFactory tempFactory();
      
}
