package harpoon.Util.Collections;

import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.AbstractMap;

/** <code>UnmodifiableMultiMap</code> is an abstract superclass to
    save developers the trouble of implementing the various mutator
    methds of the <code>MultiMap</code> interface.
*/
public abstract class UnmodifiableMultiMap 
    extends AbstractMap implements MultiMap {

    /** Constructs and returns an unmodifiable <code>MultiMap</code>
	backed by <code>mmap</code>.
    */
    public static MultiMap proxy(final MultiMap mmap) {
	return new UnmodifiableMultiMap() {
		public Object get(Object key) { 
		    return mmap.get(key); 
		}
		public Collection getValues(Object key) { 
		    return mmap.getValues(key);
		}
		public boolean contains(Object a, Object b) { 
		    return mmap.contains(a, b);
		}
		public Set entrySet() { return mmap.entrySet(); }
	    };
    }

    /** Throws UnsupportedOperationException. */
    public Object put(Object key, Object value) { die(); return null; }
    /** Throws UnsupportedOperationException. */
    public Object remove(Object key) { die(); return null; }
    /** Throws UnsupportedOperationException. */
    public boolean remove(Object key, Object value) { return die(); }
    /** Throws UnsupportedOperationException. */
    public void putAll(Map t) { die(); }
    /** Throws UnsupportedOperationException. */
    public void clear() { die(); }
    /** Throws UnsupportedOperationException. */
    public boolean add(Object key, Object value) { return die(); }
    /** Throws UnsupportedOperationException. */
    public boolean addAll(Object key, Collection values) { return die(); }
    /** Throws UnsupportedOperationException. */
    public boolean retainAll(Object key, Collection values) { return die(); }
    /** Throws UnsupportedOperationException. */
    public boolean removeAll(Object key, Collection values) { return die(); }
    private boolean die() {
	if (true) throw new UnsupportedOperationException();
	return false;
    }
}
