// NodeNotPresentInGraphException.java, created Wed Jan 13 14:36:37 1999 by pnkfelix
// Copyright (C) 1998 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.GraphColoring;

/**
 * <code>NodeNotPresentInGraphException</code>
 * 
 * @author  Felix S. Klock II <pnkfelix@mit.edu>
 * @version $Id: NodeNotPresentInGraphException.java,v 1.2 2002-02-25 20:57:17 cananian Exp $
 */

public class NodeNotPresentInGraphException extends RuntimeException {
    
    /** Creates a <code>NodeNotPresentInGraphException</code>. */
    public NodeNotPresentInGraphException() {
        super();
    }

    /** Creates a <code>NodeNotPresentInGraphException</code>. */
    public NodeNotPresentInGraphException(String s) {
        super(s);
    }
    
}
