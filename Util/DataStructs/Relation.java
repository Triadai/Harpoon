// Relation.java, created Tue Jan 11 14:52:48 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Util.DataStructs;

import java.util.Collection;
import java.util.Set;

import harpoon.Util.PredicateWrapper;

/**
 * <code>Relation</code> is a mathematical relation, accepting one to many
 * and many to one mappings.
 *
 * <p>It is similar to harpoon.Util.Collections.MultiMap but it is intended
 * to be simpler and better tailored for the implementation of the Pointer
 * Analysis algorithm.
 *
 * @author  Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
 * @version $Id: Relation.java,v 1.2 2002-02-25 21:09:19 cananian Exp $
 */
public interface Relation {
    
    /** Adds the pair <code>&lt;key, value&gt;</code> to the relation.
	Returns <code>true</code> if the new relation is bigger. */
    public boolean add(Object key, Object value);


    /** Adds a relation from <code>key</code> to each element of the set
	<code>values</code>. <code>values</code> should not contain
	duplicated elements.
	Returns <code>true</code> if the new relation is bigger. */
    public boolean addAll(Object key, Collection values);


    /** Removes the relation between <code>key</code> and 
	<code>value</code>. */ 
    public void remove(Object key, Object value);


    /** Removes the relation between <code>key</code> and 
	any element from <code>values</code>. */
    public void removeAll(Object key, Collection values);


    /** Removes all the relations attached to <code>key</code>. */
    public void removeKey(Object key);


    /** Removes all the keys that satisfy <code>predicate.check()</code>. */
    public void removeKeys(PredicateWrapper predicate);


    /** Removes all the values that satisfy <code>predicate.check()</code>. */
    public void removeValues(PredicateWrapper predicate);


    /** Removes all the relations involving at least one object that
	satisfy <code>predicate.check()</code>. */
    public void removeObjects(PredicateWrapper predicate);


    /** Checks the existence of the relation <code>&lt;key,value&gt;</code>. */
    public boolean contains(Object key, Object value);


    /** Checks the existence of the <code>key</code> key in this relation. */
    public boolean containsKey(Object key);


    /** Tests if this relation is empty or not. */
    public boolean isEmpty();


    /** Returns the image of <code>key</code> through this relation.
	The returned collection is guarranted not to contain duplicates.
	Can return <code>null</code> if no value is attached to key.
	If the result is non-null, additions and removals on the returned
	collection take effect on the relation. */
    public Set getValues(Object key);


    /** Returns all the keys appearing in this relation. The result is
	guaranted not to contain duplicates. */
    public Set keys();


    /** Returns all the values appearing in this relation. */
    public Set values();


    /** Combines <code>this</code> relation with a new one.
	A <code>null</code> parameter is considered to be an empty relation. */
    public void union(Relation rel);


    /** Checks the equality of two relations */
    public boolean equals(Object o);


    /** Returns the subrelation of this relation that contains
	only the keys that appear in <code>selected_keys</code>. */
    public Relation select(Collection selected_keys);


    /** Visits all the entries <code>&lt;key,value&gt;</code> of
	<code>this</code> relation and calls <code>visitor.visit</code>
	on each of them. */
    public void forAllEntries(RelationEntryVisitor visitor);


    /** Clones this relation. */
    public Object clone();


    /** Revert <code>this</code> relation and store the result into
    the relation <code>result</code>. &lt;a,b&gt; appears in the reverse
    relation iff &lt;b,a&gt; appears in <code>this</code> relation. */
    public void revert(Relation result);
}





















