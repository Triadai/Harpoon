// MemoryAccessError.java, created by wbeebee
// Copyright (C) 2001 Wes Beebee <wbeebee@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package javax.realtime;

/** This error is thrown on an attempt to refer to an object in an
 *  accessible <code>MemoryArea</code>. For example this will be thrown
 *  if logic in a <code>NoHeapRealtimeThread</code> attempts to refer
 *  to an object in the traditional Java heap.
 *
 * @author Wes Beebee <<a href="mailto:wbeebee@mit.edu">wbeebee@mit.edu</a>>
 */
public class MemoryAccessError extends Error {

    /** A constructor for <code>MemoryAccessError</code>. */
    public MemoryAccessError() { 
	super(); 
    }
    
    /** A descriptive constructor for <code>MemoryAccessError</code>.
     *
     *  @param s Description of the error.
     */
    public MemoryAccessError(String s) {
	super(s); 
    }
}
