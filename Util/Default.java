// Default.java, created Thu Apr  8 02:22:56 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * <code>Default</code> contains one-off or 'standard, no-frills'
 * implementations of simple <code>Iterator</code>s,
 * <code>Enumeration</code>s, and <code>Comparator</code>s.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Default.java,v 1.1.2.8 1999-11-13 18:49:39 cananian Exp $
 */
public abstract class Default  {
    /** A <code>Comparator</code> for objects that implement 
     *   <code>Comparable</code>. */
    public static final Comparator comparator = new Comparator() {
	public int compare(Object o1, Object o2) {
	    if (o1==null && o2==null) return 0;
	    // hack: in JDK1.1 String is not Comparable
	    if (o1 instanceof String && o2 instanceof String)
	       return ((String)o1).compareTo((String)o2);
	    return (o1==null) ? -((Comparable)o2).compareTo(o1):
	                         ((Comparable)o1).compareTo(o2);
	}
    };
    /** An <code>Enumerator</code> over the empty set.
     * @deprecated Use nullIterator. */
    public static final Enumeration nullEnumerator = new Enumeration() {
	public boolean hasMoreElements() { return false; }
	public Object nextElement() { throw new NoSuchElementException(); }
    };
    /** An <code>Iterator</code> over the empty set. */
    public static final Iterator nullIterator = new UnmodifiableIterator() {
	public boolean hasNext() { return false; }
	public Object next() { throw new NoSuchElementException(); }
    };
    /** An <code>Iterator</code> over a singleton set. */
    public static final Iterator singletonIterator(Object o) {
	return Collections.singleton(o).iterator();
    } 
    /** An unmodifiable version of the given iterator. */
    public static final Iterator unmodifiableIterator(final Iterator i) {
	return new UnmodifiableIterator() {
	    public boolean hasNext() { return i.hasNext(); }
	    public Object next() { return i.next(); }
	};
    }
    /** An empty map. Missing from <code>java.util.Collections</code>.*/
    public static final Map EMPTY_MAP = new Map() {
	public void clear() { }
	public boolean containsKey(Object key) { return false; }
	public boolean containsValue(Object value) { return false; }
	public Set entrySet() { return Collections.EMPTY_SET; }
	public boolean equals(Object o) {
	    if (!(o instanceof Map)) return false;
	    return ((Map)o).size()==0;
	}
	public Object get(Object key) { return null; }
	public int hashCode() { return 0; }
	public boolean isEmpty() { return true; }
	public Set keySet() { return Collections.EMPTY_SET; }
	public Object put(Object key, Object value) {
	    throw new UnsupportedOperationException();
	}
	public void putAll(Map t) {
	    if (t.size()==0) return;
	    throw new UnsupportedOperationException();
	}
	public Object remove(Object key) { return null; }
	public int size() { return 0; }
	public Collection values() { return Collections.EMPTY_SET; }
	public String toString() { return "{}"; }
    };
}
