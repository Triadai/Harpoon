// ActionRepository.java, created Mon Feb  7 15:03:29 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.PointerAnalysis;


import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Collections;

/**
 * <code>ActionRepository</code> merges together the <code>alpha</code> and
 <code>pi</code> sets from the original paper of Martin Rinard & John Whaley.
 More specifically, an <code>ActionRepository</code> maintains information
 about the actions executed by the analyzed part of the program and about the
 ordering relation between these actions and the threads that are launched
 by the analyzed part.<br>
 Currently, only two kinds of actions are supported:
 <ul>
 <li><code>ld</code> - loading of a node by reading an outside edges;
 <li><code>sync</code> - synchronization (lock acquire/release) on a node. 
 </ul>
 The implementation has been specially optimized for these two types of actions
 and for the queries that are done for synchronization removal.<br>
 Of course, there is no problem in modifying it to support new types of
 actions.
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: ActionRepository.java,v 1.1.2.10 2000-02-25 01:06:12 salcianu Exp $
 */
public class ActionRepository {
    
    /** Fake thread node used as the author thread for the actions done
	by the main thread of the analyzed procedure, not by the one of
	the threads it is starting. */
    public static final PANode THIS_THREAD = new PANode(PANode.INSIDE);

    /** Creates a <code>ActionRepository</code>. */
    public ActionRepository() {
        alpha_ld   = new HashSet();
	pi_ld      = new Relation();
	alpha_sync = new Relation();
	pi_sync    = new Hashtable();
    }


    // Set<PALoad>
    HashSet  alpha_ld;
    // Relation<PANode,PALoad>
    Relation pi_ld;

    /** Adds a load action in parallel with all the 
	<code>active_threads</code>. */
    private void add_ld(PALoad load, Set active_threads){
	alpha_ld.add(load);

	if(active_threads == null) return;
	
	Iterator it = active_threads.iterator();
	while(it.hasNext())
	    pi_ld.add((PANode)it.next(), load);
    }

    /** Adds a <code>ld</code> action.
	The thread <code>nt</code> read the outside edge from <code>n1</code>
	to <code>n2</code> through field <code>f</code>, in parallel with
	all the threads from <code>active_threads</code>. */
    public void add_ld(PANode n1, String f, PANode n2, PANode nt,
		       Set active_threads){
	add_ld(new PALoad(n1,f,n2,nt),active_threads);
    }

    /** Convenient function.
	It iterates over <code>set_n1</code>, repeatedly calling
	<code>add_ld(n1,f,n2,nt,active_threads)</code> for each
	<code>n1</code> in <code>set_n1</code>. */
    public void add_ld(Set set_n1, String f, PANode n2, PANode nt,
		       Set active_threads){
	Iterator it_n1 = set_n1.iterator();
	while(it_n1.hasNext()){
	    PANode n1 = (PANode) it_n1.next();
	    add_ld(n1,f,n2,nt,active_threads);
	}
    }

    /** Convenient function.
	It iterates over <code>set_nt</code>, repeatedly calling
	<code>add_ld(set_n1,f,n2,nt,active_threads)</code> for each
	<code>nt</code> in <code>set_nt</code>. */
    public void add_ld(Set set_n1, String f, PANode n2, Set set_nt,
			Set active_threads){

	//System.out.println("set_n1 = " + set_n1);
	//System.out.println("f      = " + f);
	//System.out.println("n2     = " + n2);
	//System.out.println("set_nt = " + set_nt);
	//System.out.println("active_threads = " + active_threads);

	Iterator it_nt = set_nt.iterator();
	while(it_nt.hasNext()){
	    PANode nt = (PANode) it_nt.next();
	    add_ld(set_n1,f,n2,nt,active_threads);
	}
    }


    // Relation<PANode,PANode>;   n -> nt (nt does a sync on n)
    Relation  alpha_sync;
    // Hashtable<PANode,Relation<PANode,PANode>>
    // nt2 -> n -> nt1 (nt1 does a sync on n in // with the thread nt2)
    Hashtable pi_sync;
    
    /** Adds a <code>sync</code> action.
	The thread <code>nt</code> synchronized on <code>n</code> in
	parallel with all the threads from <code>active_threads</code>. */
    public void add_sync(PANode n, PANode nt, Set active_threads){
	alpha_sync.add(n,nt);
	
	if((active_threads == null) || active_threads.isEmpty()) return;

	if(PointerAnalysis.DEBUG2){
	    System.out.print("WONDERFUL: < sync , " + n + " , " + 
			     nt + " > || [");
	    
	    Iterator it2 = active_threads.iterator();
	    while(it2.hasNext())
		System.out.print(it2.next() + " ");
	    
	    System.out.println("]");
	}

	Iterator it = active_threads.iterator();
	while(it.hasNext()){
	    PANode nt2 = (PANode) it.next();
	    
	    Relation rel = (Relation) pi_sync.get(nt2);
	    if(rel == null){
		rel = new Relation();
		pi_sync.put(nt2,rel);
	    }

	    rel.add(n,nt);
	}
    }

    /** Convenient function.
	It iterates over <code>set_n</code> and <code>set_nt</code>,
	repeatedly calling <code>add_sync(n,nt,active_threads)</code> for each
	<code>n</code> in <code>set_n</code> and each 
	<code>nt</code> in <code>set_nt</code>. */ 
    public void add_sync(Set set_n, Set set_nt, Set active_threads){

	//System.out.println("set_n  = " + set_n);
	//System.out.println("set_nt = " + set_nt);
	//System.out.println("active_threads = " + active_threads);

	Iterator it_n = set_n.iterator();
	while(it_n.hasNext()){
	    PANode n = (PANode) it_n.next();
	    Iterator it_nt = set_nt.iterator();
	    while(it_nt.hasNext()){
		PANode nt = (PANode) it_nt.next();
		add_sync(n,nt,active_threads);
	    }
	}
    }

    /** Checks the equality of two <code>ActionRepository</code>s. */
    public boolean equals(Object o){
	ActionRepository ar2 = (ActionRepository)o;
	if (this == ar2) return true;
	return
	    alpha_ld.equals(ar2.alpha_ld) &&
	    alpha_sync.equals(ar2.alpha_sync) &&
	    pi_ld.equals(ar2.pi_ld) &&
	    pi_sync.equals(ar2.pi_sync);
    }

    /** Adds the information about actions and action-thread ordering from
	<code>ar2</code> to <code>this</code> action repository.
	This method is called in the control-flow join points. */
    public void join(ActionRepository ar2){
	// these first three are really easy
	alpha_ld.addAll(ar2.alpha_ld);
	pi_ld.union(ar2.pi_ld);
	alpha_sync.union(ar2.alpha_sync);
	// this one is a bit more difficult; we have to do it by hand
	Iterator  it = ar2.pi_sync.entrySet().iterator();
	while(it.hasNext()){
	    Map.Entry entry = (Map.Entry) it.next();
	    PANode    nt2 = (PANode)   entry.getKey();
	    Relation rel2 = (Relation) entry.getValue();
	    Relation rel1 = (Relation) pi_sync.get(nt2);
	    if(rel1 == null)
		pi_sync.put(nt2,(Relation) rel2.clone());
	    else
		rel1.union(rel2);
	}
    }

    /** Visits all the actions from this repository.
	It calls <code>visitor.visit_ld</code> on the <code>ld</code> actions
	and <code>visitor.visit_sync</code> on the <code>sync</code>. */
    public void forAllActions(ActionVisitor visitor){
	// visit all the "ld" actions
	Iterator it_ld = alpha_ld.iterator();
	while(it_ld.hasNext()){
	    PALoad load = (PALoad) it_ld.next();
	    visitor.visit_ld(load);
	}
	// visit all the "sync" actions
	Enumeration enum_n = alpha_sync.keys();
	while(enum_n.hasMoreElements()){
	    PANode n = (PANode) enum_n.nextElement();
	    Iterator it_nt = alpha_sync.getValues(n);
	    while(it_nt.hasNext()){
		PANode nt = (PANode) it_nt.next();
		visitor.visit_sync(n,nt);
	    }
	}
    }


    /** Visits all the "parallel action" items of information from this
	repository (i.e. all the <code> action || thread </code> elements.
	It calls <code>visitor.visit_par_ld</code> or 
	<code>visitor.visit_par_sync</code> according to the type of the
	<code>action</code>. */
    public void forAllParActions(ParActionVisitor visitor){
	// visit all the "ld" || nt elements
	Enumeration enum_nt2 = pi_ld.keys();
	while(enum_nt2.hasMoreElements()){
	    PANode nt2 = (PANode) enum_nt2.nextElement();
	    Iterator it_loads = pi_ld.getValues(nt2);
	    while(it_loads.hasNext()){
		PALoad load = (PALoad) it_loads.next();
		visitor.visit_par_ld(load,nt2);
	    }
	}
	// visit all the "sync" || nt elements
	enum_nt2 = pi_sync.keys();
	while(enum_nt2.hasMoreElements()){
	    PANode nt2 = (PANode) enum_nt2.nextElement();
	    Relation rel = (Relation) pi_sync.get(nt2);
	    Enumeration enum_n = rel.keys();
	    while(enum_n.hasMoreElements()){
		PANode n = (PANode) enum_n.nextElement();
		Iterator it_nt1 = rel.getValues(n);
		while(it_nt1.hasNext()){
		    PANode nt1 = (PANode) it_nt1.next();
		    visitor.visit_par_sync(n,nt1,nt2);
		}
	    }
	}
    }


    /** Returns an iterator over the set of loads that are performed in 
	parallel with an <code>nt</code> thread. <code>O(1)</code> time. */
    public Iterator parallelLoads(PANode nt){
	return pi_ld.getValues(nt);
    }


    /** Removes all the information related to edges containing nodes from
	<code>nodes</code>. */
    public void removeNodes(final Set nodes){

	// clean alpha_ld
	Iterator it_paload = alpha_ld.iterator();
	while(it_paload.hasNext()){
	    PALoad load = (PALoad) it_paload.next();
	    if(load.isBad(nodes)) alpha_ld.remove(load);
	}

	PredicateWrapper node_predicate = new PredicateWrapper(){
		public boolean check(Object obj){
		    return nodes.contains((PANode) obj);
		}		
	    };

	// clean pi_ld
	pi_ld.removeKeys(node_predicate);
	pi_ld.removeValues(new PredicateWrapper(){
		public boolean check(Object obj){
		    return ((PALoad) obj).isBad(nodes);
		}
	    });

	// clean alpha_sync
	alpha_sync.removeObjects(node_predicate);

	// clean pi_sync
	Iterator it_nodes = nodes.iterator();
	while(it_nodes.hasNext())
	    pi_sync.remove((PANode) it_nodes.next());

	it_nodes = pi_sync.keySet().iterator();
	while(it_nodes.hasNext()){
	    PANode node  = (PANode) it_nodes.next();
	    Relation rel = (Relation) pi_sync.get(node);
	    rel.removeObjects(node_predicate);
	    if(rel.isEmpty()) pi_sync.remove(node);
	}
	
    }

    /** Removes all the information related to <code>ld</code> actions
	using the edges from <code>edges</code>.
	<code>edges</code> is supposed to be a set of <code>PAEdge</code>s. */
    public void removeEdges(final Set edges){

	// clean alpha_ld
	Iterator it_paload = alpha_ld.iterator();
	while(it_paload.hasNext()){
	    PALoad load = (PALoad) it_paload.next();
	    if(edges.contains(new PAEdge(load.n1,load.f,load.n2)))
		alpha_ld.remove(load);
	}

	// clean pi_ld
	pi_ld.removeValues(new PredicateWrapper(){
		public boolean check(Object obj){
		    PALoad load = (PALoad) obj;
		    return edges.contains(new PAEdge(load.n1,load.f,load.n2));
		}
	    });
    }

    /** Checks if all the <code>sync</code> operation on <code>n</code>
	are independent (temporarily speaking) or not. 
	If it returns <code>false</code>, two threads could synchonize
	on it at the same time (i.e. they can simultaneously access it);
	in this case, the synchonizations are really necessary and should
	NOT be removed. */
    public boolean independent(PANode n){
	// Goes through all the threads nt2 that are synchronizing on n
	Iterator it_threads = alpha_sync.getValues(n);
	while(it_threads.hasNext()){
	    PANode nt2 = (PANode) it_threads.next();
	    if(nt2 == THIS_THREAD) continue;
	    // Find the set of all the threads nt1 that synchronize 
	    // on n in || with nt2
	    Set concurent_syncs = ((Relation)pi_sync.get(nt2)).getValuesSet(n);
	    // If there are such threads, we can have concurrent accesses.
	    if(!concurent_syncs.isEmpty())
		return false;
	}
	return true;
    }


    // Private constructor for clone and keepTheEssential
    private ActionRepository(HashSet alpha_ld, Relation pi_ld,
			     Relation alpha_sync, Hashtable pi_sync){
	this.alpha_ld   = alpha_ld;
	this.pi_ld      = pi_ld;
	this.alpha_sync = alpha_sync;
	this.pi_sync    = pi_sync;
    }


    // TODO: different internal function for add_alpha and add_pi
    // (so that we don't rely on Colections.singleton) You will
    // understand

    /** Produces an <code>ActionRepository</code> containing only the \
	nodes that could be reached from the outside.
	(i.e. via parameters,
	class nodes, normally or exceptionally returned nodes or the
	started thread nodes) */
    private ActionRepository keepTheEssential(final Set remaining_nodes){
	final ActionRepository _ar = new ActionRepository();

	forAllActions(new ActionVisitor(){
		public void visit_ld(PALoad load){
		    if(load.isGood(remaining_nodes))
			_ar.add_ld(load,Collections.EMPTY_SET);
		}
		public void visit_sync(PANode n, PANode nt){
		    if(remaining_nodes.contains(n) &&
		       remaining_nodes.contains(nt))
			_ar.add_sync(n,nt,Collections.EMPTY_SET);
		}
	    });
	
	forAllParActions(new ParActionVisitor(){
		public void visit_par_ld(PALoad load, PANode nt2){
		    if(load.isGood(remaining_nodes) &&
		       remaining_nodes.contains(nt2))
			_ar.add_ld(load,Collections.singleton(nt2));
		}
		public void visit_par_sync(PANode n, PANode nt1, PANode nt2){
		    if(remaining_nodes.contains(n) &&
		       remaining_nodes.contains(nt1) &&
		       remaining_nodes.contains(nt2))
			_ar.add_sync(n,nt1,Collections.singleton(nt2));
		}
	    });

	return _ar;
    }



    /** Produce a copy of <code>this</code> object. 
	The new object is totally independent from the old one: you can
	add/remove actions to/from it without affecting the original. */ 
    public Object clone(){
	HashSet  new_alpha_ld   = (HashSet) alpha_ld.clone();
	Relation new_pi_ld      = (Relation) pi_ld.clone();
	Relation new_alpha_sync = (Relation) alpha_sync.clone();

	Hashtable new_pi_sync   = new Hashtable();
	Iterator  it = pi_sync.entrySet().iterator();
	while(it.hasNext()){
	    Map.Entry entry = (Map.Entry) it.next();
	    PANode   nt2  = (PANode)   entry.getKey();
	    Relation rel2 = (Relation) entry.getValue();
	    new_pi_sync.put(nt2,(Relation) rel2.clone());
	}

	return new ActionRepository(new_alpha_ld, new_pi_ld,
				    new_alpha_sync, new_pi_sync);
    }


    /** Pretty-printer for debug purposes. 
	<code>ar1.equals(ar2) <==> ar1.toString().equals(ar2.toString()).</code>*/
    public String toString(){
	StringBuffer buffer = new StringBuffer();
	final Set strings = new HashSet();

	ActionVisitor act_visitor = new ActionVisitor(){
		public void visit_ld(PALoad load){
		    strings.add("  " + load + "\n");
		}
		public void visit_sync(PANode n, PANode nt){
		    strings.add("  < sync , " + n + 
				(nt!=THIS_THREAD?(" , " + nt):"") +
				" >\n");
		}
	    };

	buffer.append(" Alpha:\n");
	forAllActions(act_visitor);

	Object[] strs = Debug.sortedSet(strings);
	for(int i = 0 ; i < strs.length ; i++)
	    buffer.append(strs[i]);

	strings.clear();

	ParActionVisitor par_act_visitor = new ParActionVisitor(){
		public void visit_par_ld(PALoad load, PANode nt2){
		    strings.add("  " + load + " || " + nt2 + "\n");
		}
		public void visit_par_sync(PANode n, PANode nt1, PANode nt2){
		    strings.add("  < sync , " + n + 
				(nt1!=THIS_THREAD?(" , " + nt1):"") +
				" > || " + nt2 + "\n");		    
		}
	    };

	buffer.append(" Pi:\n");
	forAllParActions(par_act_visitor);

	strs = Debug.sortedSet(strings);
	for(int i = 0 ; i < strs.length ; i++)
	    buffer.append(strs[i]);

	return buffer.toString();
    }
}


