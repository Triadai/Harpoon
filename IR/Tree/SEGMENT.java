// SEGMENT.java, created Tue Jul 27 12:43:42 1999 by duncan
// Copyright (C) 1998 Duncan Bryce <duncan@lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.ClassFile.HCodeElement;
import harpoon.Temp.TempMap;
import harpoon.Util.Util;

import java.util.Collections;
import java.util.Set;

/**
 *  The <code>SEGMENT</code> class is used to mark the beginning of a new
 *  section of memory.  All subsequent <code>Tree</code> objects will be 
 *  stored in the specified section.  
 * 
 * @author  Duncan Bryce <duncan@lcs.mit.edu>
 * @version $Id: SEGMENT.java,v 1.1.2.20 2000-02-15 17:19:04 cananian Exp $
 */
public class SEGMENT extends Stm implements harpoon.ClassFile.HDataElement {
    /** R/O storage for static class data (display, vmtable, etc) */
    public static final int CLASS                = 0; 
    /** Read-only instruction memory */
    public static final int CODE                 = 1;
    /** R/O storage for GC tables */
    public static final int GC                   = 2;
    /** R/W memory that must be initialized before use */
    public static final int INIT_DATA            = 3;
    /** R/O memory that stores reflection data objects. */
    public static final int REFLECTION_OBJECTS   = 4;
    /** R/O memory that stores reflection tables and other non-object data. */
    public static final int REFLECTION_DATA      = 5;
    /** R/W storage for static aggregate data */
    public static final int STATIC_OBJECTS       = 6;
    /** R/W storage for static primitive data */
    public static final int STATIC_PRIMITIVES    = 7;
    /** R/O storage for string constant objects */
    public static final int STRING_CONSTANTS     = 8;
    /** R/O storage for component character arrays of statically allocated 
     *  string constant objects. */
    public static final int STRING_DATA          = 9;
    /** Read-only memory (other than machine instructions) */
    public static final int TEXT                 = 10; 
    /** R/W memory initialized at load time to be 0 */
    public static final int ZERO_DATA            = 11; 
    /** R/O storage for GC index */
    public static final int GC_INDEX             = 12;

    /** Converts a segtype into its string representation.
     */
    public static final String decode(int segtype) {
	switch(segtype) {
	case CLASS:               return "CLASS";
	case CODE:                return "CODE";
	case GC:                  return "GC";
	case INIT_DATA:           return "INIT_DATA";
	case REFLECTION_OBJECTS:  return "REFLECTION_OBJECTS";
	case REFLECTION_DATA:     return "REFLECTION_DATA";
	case STATIC_OBJECTS:      return "STATIC_OBJECTS";
	case STATIC_PRIMITIVES:   return "STATIC_PRIMITIVES";
	case STRING_CONSTANTS:    return "STRING_CONSTANTS";
	case STRING_DATA:         return "STRING_DATA";
	case TEXT:                return "TEXT";
	case ZERO_DATA:           return "ZERO_DATA";
	case GC_INDEX:            return "GC_INDEX";
	default: 
	    Util.assert(false, "Unknown segment type "+segtype); 
	    return null;
	}
    }

    /** The type of segment this <code>SEGMENT</code> precedes. */
    public final int segtype; 

    /** Class constructor. 
     *  <code>segtype</code> must be one the segments types specified
     *  in this class. */
    public SEGMENT(TreeFactory tf, HCodeElement source, int segtype) { 
	super(tf, source, 0); 
	this.segtype = segtype;
	Util.assert(segtype>=0 && segtype<12);
    }
    
    protected Set defSet() { return Collections.EMPTY_SET; }
    protected Set useSet() { return Collections.EMPTY_SET; }
    
    public int     kind()  { return TreeKind.SEGMENT; }

    public Stm build(TreeFactory tf, ExpList kids) { 
	return new SEGMENT(tf, this, segtype); 
    } 

    /** Accept a visitor */
    public void accept(TreeVisitor v) { v.visit(this); }

    public Tree rename(TreeFactory tf, TempMap tm, CloneCallback cb) {
        return cb.callback(this, new SEGMENT(tf, this, segtype), tm);
    }

    public String toString() {
	return "SEGMENT<" + decode(segtype) + ">";
    }
}
