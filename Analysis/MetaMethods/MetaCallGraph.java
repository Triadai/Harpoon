// MetaCallGraph.java, created Mon Mar 13 15:53:31 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.MetaMethods;

import java.util.Set;

import java.io.PrintStream;

import harpoon.IR.Quads.CALL;

import harpoon.Util.DataStructs.Relation;
import harpoon.Util.DataStructs.LightRelation;
import harpoon.Util.DataStructs.RelationEntryVisitor;

import harpoon.Util.Graphs.SCComponent;
import harpoon.Util.Graphs.Navigator;
import harpoon.Util.Graphs.ReverseNavigator;
import harpoon.Util.Graphs.SCCTopSortedGraph;

/**
 * <code>MetaCallGraph</code> is for meta methods what <code>callGraph</code>
 is for &quot;normal&quot; methods. It provides information on what meta
 methods are called by a given meta method [at a specific call site].
 * 
 * @author  Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
 * @version $Id: MetaCallGraph.java,v 1.4 2003-04-30 21:24:44 salcianu Exp $
 */

public abstract class MetaCallGraph implements java.io.Serializable {
    
    /** Returns the meta methods that can be called by <code>mm</code>. */
    public abstract MetaMethod[] getCallees(MetaMethod mm);
    
    /** Returns the meta methods that can be called by <code>mm</code>
	at the call site <code>q</code>. */
    public abstract MetaMethod[] getCallees(MetaMethod mm, CALL cs);

    /** Returns the set of all the call sites in the code of the meta-method
	<code>mm</code>. */
    public abstract Set getCallSites(MetaMethod mm);
    
    /** Returns the set of all the meta methods that might be called during the
	execution of the program. */
    public abstract Set getAllMetaMethods();

    /** Returns the set of all the meta methods that might be called, directly
	or indirectly, by the meta method <code>mm</code>. It's just the
	transitive closure of the <code>getCallees</code> method. */
    public abstract Set getTransCallees(MetaMethod mm);

    /** Computes the <i>split</i> relation. This is a <code>Relation</code>
	that associates to each <code>HMethod</code> the set of
	<code>MetaMethod</code>s specialized from it. */
    public abstract Relation getSplitRelation();

    /** Returns the set of the meta-methods that could be called as the 
	body of some thread. */
    public abstract Set getRunMetaMethods();

    /** Nice pretty-printer for debug purposes. */
    public abstract void print(PrintStream ps, boolean detailed_view,
			       MetaMethod root);


    /** Returns a bi-directional top-down graph navigator through
        <code>this</code> meta-callgraph. */
    private Navigator getTopDownNavigator() {
	final MetaAllCallers mac = new MetaAllCallers(this);
	
	return new Navigator() {
	    public Object[] next(Object node) {
		return getCallees((MetaMethod) node);
	    }  
	    public Object[] prev(Object node) {
		return mac.getCallers((MetaMethod) node);
	    }
	};
    }


    /** Constructs a top-down topologically sorted view of
	<code>this</code> meta-callgraph.  It starts with the strongly
	connected component for the main method and ends with the
	strongly connected components for the leaf methods. */
    public SCCTopSortedGraph getTopDownSortedView() {
	Set/*<MetaMethod>*/ allmms = getAllMetaMethods();
	return
	    SCCTopSortedGraph.topSort
	    (SCComponent.buildSCC
	     (allmms.toArray(new Object[allmms.size()]),
	      getTopDownNavigator()));
    }

    /** Constructs a bottom-up topologically sorted view of
	<code>this</code> meta-callgraph.  It starts with the strongly
	connected components for the leaf methods and ends with the
	strongly connected components for the main methods. */
    public SCCTopSortedGraph getBottomUpSortedView() {
	Set/*<MetaMethod>*/ allmms = getAllMetaMethods();
	return
	    SCCTopSortedGraph.topSort
	    (SCComponent.buildSCC
	     (allmms.toArray(new Object[allmms.size()]),
	      new ReverseNavigator(getTopDownNavigator())));
    }
}
