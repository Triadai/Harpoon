// MultiMap.java, created Tue Oct 19 22:19:36 1999 by pnkfelix
// Copyright (C) 1999 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Util.Collections;

import harpoon.Util.Util;
import harpoon.Util.PairMapEntry;
import harpoon.Util.UnmodifiableIterator;
import harpoon.Util.CombineIterator;
import harpoon.Util.FilterIterator;

import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.HashSet;

/** <code>MultiMap</code> maps a key to a collection of values.  These
    collections are created as needed using a
    <code>CollectionFactory</code>.  Any constraints on the
    collections produced by this factory thus hold for the values that
    <code>this</code> maps to.

    <BR> Formally, a MultiMap is a <i>Multiple Associative
    Container</i>.  It associates key objects with value objects.  The
    difference between a <code>MultiMap</code> and a standard 
    <code>Map</code> is that <code>MultiMap</code> extends the
    <code>Map</code> interface to allow for the same key to map to
    multiple values. 

    <BR> Thus, the type signature for a MultiMap is :: 
         Map[keytype -> [valtype] ]
    
    <BR> Note that an association (known as a (Key, Value) pair or a 
         <code>Map.Entry</code> in the Java Collections API) is only 
	 defined to exist if the collection of objects mapped to by
	 some key is non-empty. 
    
	 This has a number of implications for the behavior of
	 <code>MultiMap</code>:

    <BR> Let <OL>
         <LI> <code>mm</code> be a <code>MultiMap</code>,
	 <LI> <code>k</code> be an <code>Object</code> (which may or may
	          not be a Key in <code>mm</code>)
	 <LI> <code>c</code> be the <code>Collection</code> returned by
	          <code>mm.getValues(k)</code>.
    </OL>
    <BR> Then <code>c</code> will either be a non-empty
         <code>Collection</code> (the case where <code>k</code> is a
	 Key in <code>mm</code> or it will be an empty collection (the
	 case where <code>k</code> is not a Key in <code>mm</code>.
	 In this case, however, <code>k</code> is still considered to
	 not be a Key in <code>mm</code> until <code>c</code> is made
	 non-empty.  We chose to return an empty
	 <code>Collection</code> instead of <code>null</code> to allow
	 for straightforward addition to the collection of values
	 mapped to by <code>k</code>.

    <BR> To conform to the <code>Map</code> interface, the
         <code>put(key, value)</code> method has a non-intuitive
	 behavior; it throws away all values previously associated
	 with <code>key</code> and creates a new mapping from
	 <code>key</code> to the singleton set containing
	 <code>value</code>. 

    <P>  Note that the behavior of <code>MultiMap</code> is
         indistinquishable from that of a <code>Map</code> if none of
	 the extensions of <code>MultiMap</code> are utilized.  Thus,
	 users should take care to ensure that other code relying on
	 the constraints enforced by the <code>Map</code> interface
	 does not ever attempt to use a <code>MultiMap</code> when any
	 of its Keys map to more than one value.

    <P>  Also, right now the implementation tries to preserve the
         property that if a key 'k' maps to an empty collection 'c' in
	 some MultiMap 'mm', then users of 'mm' will not be able to
	 see that 'k' is a member of the keySet for 'mm'.  However, it
	 does not preserve this property when mm.getValues(k) is used
	 as a means to operate on the state of 'mm', and it is not
	 clear to me whether one can even ensure that the property
	 can be maintained if arbitrary operations on mm.getValues(k)
	 are passed on to 'mm'.

    <P>  This data structure is a bit experimental; a few changes may
         be coming:<OL>
	 <LI> We may make it not extend the <code>Map</code>
	      interface, because it inherently violates the
	      constraints of the <code>Map</code> interface once
	      multiple values are added for one key.
         <LI> The <code>Collection</code> views returned right now
	      don't offer very much in terms of modifying the
	      state of <code>this</code> internally.
	 <LI> Some of the views returned do not properly reflect
	      modification in <code>this</code>.  This is a gross
	      oversight of <code>Collection</code>'s interface
	      on my part and I need to fix it, which I will do when I
	      have free time.
	 </OL> 
    
    @author  Felix S. Klock II <pnkfelix@mit.edu>
    @version $Id: MultiMap.java,v 1.1.2.12 2000-02-28 06:41:57 cananian Exp $
 */
public interface MultiMap extends Map, harpoon.Util.BinaryRelation {

    /** <code>MultiMap.Factory</code> is a <code>MultiMap</code>
	generator. 
     */
    public static class Factory extends MapFactory {
	public Map makeMap(Map map) {
	    return makeMultiMap(map);
	}
	
	/** Creates a new <code>MultiMap</code> initialized with all 
	    of the <code>Map.Entry</code>s in <code>map</code>
	 */
	public MultiMap makeMultiMap(Map map) {
	    return new GenericMultiMap(map);
	}

	public MultiMap makeMultiMap(CollectionFactory cf, 
				     MapFactory mf) {
	    return new GenericMultiMap(cf, mf);
	}
    } 


    /** Returns some arbitrary value from the set of values to which
	this map maps the specified key.  Returns <code>null</code> if
	the map contains no mapping for the key; it's also possible
	that the map explicitly maps the key to <code>null</code>.
	The <code>containsKey</code> operation may be used to
	distinquish these two cases.
	
	Note that if only the <code>put</code> method is used to
	modify <code>this</code>, then <code>get</code> will operate
	just as it would in any other <code>Map</code>.
    */
    public Object get(Object key);

    /** Associates the specified value with the specified key in this
	map, after removing all old values associated with the key.
	Returns some value that was previously associated with the
	specified key, or <code>null</code> if no values were
	associated previously.  */
    public Object put(Object key, Object value);

    /** Copies the mappings from the specified map to this
	map, after removing all old values associated with the key.  Note
	that <code>putAll(mm)</code> where <code>mm</code> is a
	<code>MultiMap</code> will NOT add all of the mappings in
	<code>mm</code>; it will only add all of the Keys in
	<code>mm</code>, mapping each Key to one of the Values it
	mapped to in <code>mm</code>.  To add all of the mappings from
	another <code>MultiMap</code>, use
	<code>addAll(MultiMap)</code>.
    */
    public void putAll(Map t);
    
    /** Ensures that <code>this</code> contains an association from
	<code>key</code> to <code>value</code>.

	(<code>MultiMap</code> specific operation).

	@return <code>true</code> if this mapping changed as a result of
	        the call
    */
    boolean add(Object key, Object value);

    /** Adds to the current mappings: associations for
	<code>key</code> to each value in <code>values</code>.  

	(<code>MultiMap</code> specific operation). 

	@return <code>true</code> if this mapping changed as a result
	        of the call
    */
    boolean addAll(Object key, Collection values);


    /** Removes from the current mappings: associations for
	<code>key</code> to any value not in <code>values</code>. 

	(<code>MultiMap</code> specific operation). 

	@return <code>true</code> if this mapping changed as a result
	        of the call
    */
    boolean retainAll(Object key, Collection values);

    /** Removes from the current mappings: associations for
	<code>key</code> to any value in <code>values</code>.

	(<code>MultiMap</code> specific operation). 

	@return <code>true</code> if this mapping changed as a result
	        of the call
    */
    boolean removeAll(Object key, Collection values);

    /** Returns the collection of Values associated with
	<code>key</code>.  Modifications to the returned
	<code>Collection</code> affect <code>this</code> as well.  If 
	there are no Values currently associated with
	<code>key</code>, constructs a new, mutable, empty
	<code>Collection</code> and returns it.
	(<code>MultiMap</code> specific operation). 
    */
    Collection getValues(Object key);

} 


