// LinearSet.java, created Wed Aug  4 12:03:54 1999 by pnkfelix
// Copyright (C) 1999 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>LinearSet</code> is a simplistic light-weight
 * <code>Set</code> designed for use when the number of entries is
 * small.  It is backed by an <code>ArrayList</code>.
 * 
 * @author  Felix S. Klock II <pnkfelix@mit.edu>
 * @version $Id: LinearSet.java,v 1.1.2.3 1999-08-18 17:32:54 pnkfelix Exp $
 */
public class LinearSet extends AbstractSet {
    private ArrayList list;

    /** Creates a <code>LinearSet</code>. */
    public LinearSet() {
        list = new ArrayList();
    }

    /** Creates a <code>LinearSet</code> with given capacity. */
    public LinearSet(int capacity) {
        list = new ArrayList(capacity);
    }
    
    public Iterator iterator() {
	return list.iterator();
    }

    public int size() {
	return list.size();
    }

    public boolean add(Object o) {
	if (list.contains(o)) {
	    return false;
	} else {
	    list.add(o);
	    return true;
	}
    }

    public boolean remove(Object o) {
	int index = list.indexOf(o);
	if (index == -1) {
	    return false;
	} else {
	    list.remove(index);
	    return true;
	}
    }
}
