// ParIntGraph.java, created Sun Jan  9 15:40:59 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.PointerAnalysis;


import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.LinkedList;


import harpoon.Temp.Temp;
import harpoon.IR.Quads.CALL;
import harpoon.Analysis.MetaMethods.MetaMethod;
import harpoon.Util.Util;

import harpoon.Util.DataStructs.Relation;
import harpoon.Util.DataStructs.LightRelation;
import harpoon.Util.DataStructs.RelationEntryVisitor;


/**
 * <code>ParIntGraph</code> models a Parallel Interaction Graph data
 structure. Most of its fields retain the original name from the paper
 of Martin and John Whaley.
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: ParIntGraph.java,v 1.1.2.37 2000-11-16 03:01:12 salcianu Exp $
 */
public class ParIntGraph {

    public static boolean DEBUG = false;
    public static boolean DEBUG2 = false;

    /** Debug for the aggressive shrinking. */
    public static boolean DEBUG_AS = false;

    /** Activates the aggressive shrinking. Buggy for the moment ... */
    public static boolean AGGRESSIVE_SHRINKING = true;

    /** Default (empty) graph. It doesn't contain any information.  */
    public static final ParIntGraph EMPTY_GRAPH = new ParIntGraph();

    /** Points-to escape graph that summarizes the points-to and escape
	information for the current thread. */
    public PointsToGraph G;
    
    /** The paralel thread map; it attaches to each thread node nT, an 
	integer from the set {0,1,2} where 2 signifies the possibility
	that multiple instances of nT execute in parallel with the current
	thread. */
    public PAThreadMap tau;
    
    /** The set of thread objects that are accessed after they are started.
	This is not exactly part of the PA, but we need it for the thread
	specific heaps; Martin intend to allocate even the thread object in
	the thread heap - it is atomically collected at the end of the thread.
	For the moment, this info is accurate only for captured nodes
	(it is not carried over in the inter-procedural phase). */
    public Set touched_threads;

    /** Maintains the actions executed by the analysed code and the parallel
	action relation. <code>alpha</code> and <code>pi</code> from the 
	original paper have been merged into this single field for efficiency
	reasons. */
    public ActionRepository ar;

    /** Maintains the (conservative) ordering relations between the inside
	and the outside edges. <code>before(ei,eo)</code> is true if the
	inside edge ei might be created before the ouside edge eo was read. */
    public EdgeOrdering eo;
    
    /** Creates a <code>ParIntGraph</code>. */
    public ParIntGraph() {
	G   = new PointsToGraph();
	tau = new PAThreadMap();
	ar  = new ActionRepository();
	
	eo = PointerAnalysis.IGNORE_EO ? null : new EdgeOrdering();

	touched_threads = new HashSet();
    }
    

    /** <code>join</code> combines two <code>ParIntGraph</code>s in \
	a control-flow join point. */
    public void join(ParIntGraph pig2){
	if(pig2 == null) return;

	G.join(pig2.G);
	tau.join(pig2.tau);
	ar.join(pig2.ar);

	if(!PointerAnalysis.IGNORE_EO)
	    eo.join(pig2.eo);

	touched_threads.addAll(pig2.touched_threads);
    }


    /** Inserts the image of <code>pig2</code> parallel interaction graph
	through the <code>mu</code> node mapping into <code>this</code> object.
	This method is designed to be called at the end of the caller/callee
	or starter/startee interaction. It is *not* manipulating the action
	repository nor the edge ordering; those manipulations are too complex
	and variate to be done here.<br>
	<code>principal</code> controls whether the return and exception set
	are inserted. */
    void insertAllButArEo(ParIntGraph pig2, Relation mu,
			boolean principal, Set noholes){
	G.insert(pig2.G, mu, principal, noholes);
	tau.insert(pig2.tau, mu);
    }

    /** Convenient function equivalent to 
	<code>insertAllButArEo(pig2,mu,principal,Collections.EMPTY_SET). */
    void insertAllButArEo(ParIntGraph pig2, Relation mu, boolean principal){
	insertAllButArEo(pig2,mu,principal,Collections.EMPTY_SET);
    }


    /** Check the equality of two <code>ParIntGraph</code>s. */
    public boolean equals(Object obj){
	if(obj == null) return false;

	ParIntGraph pig2 = (ParIntGraph) obj;
	if(!G.equals(pig2.G)){
	    if(DEBUG2)
		System.out.println("The graphs are different");
	    return false;
	}
	if(!tau.equals(pig2.tau)){
	    if(DEBUG2)
		System.out.println("The tau's are different");
	    return false;
	}
	if(!ar.equals(pig2.ar)){
	    if(DEBUG2){
		System.out.println("The ar's are different");
		ar.show_evolution(pig2.ar);
	    }
	    return false;
	}

	if(!PointerAnalysis.IGNORE_EO)
	    if(!eo.equals(pig2.eo)){
		if(DEBUG2)
		    System.out.println("The eo's are different");
		return false;
	    }

	if(!touched_threads.equals(pig2.touched_threads)){
	    if(DEBUG2)
		System.out.println("The touched_thread's are different");
	    return false;
	}
	return true;
    }

    /** Private constructor for <code>clone</code> and 
	<code>keepTheEssential</code>. */
    private ParIntGraph(PointsToGraph G, PAThreadMap tau, ActionRepository ar,
			EdgeOrdering eo, Set touched_threads){
	this.G   = G;
	this.tau = tau;
	this.ar  = ar;
	this.eo  = eo;
	this.touched_threads = touched_threads;
    }

    /** Records the fact that the started thread nt is accessed by its 
	direct startee. */
    public final void touch_thread(PANode nt){
	touched_threads.add(nt);
    }

    /** <code>clone</code> produces a copy of the <code>this</code>
	Parallel Interaction Graph. */
    public Object clone(){

	EdgeOrdering _eo = 
	    PointerAnalysis.IGNORE_EO ? null : (EdgeOrdering)eo.clone();

	return new ParIntGraph((PointsToGraph)G.clone(),
			       (PAThreadMap)tau.clone(),
			       (ActionRepository)ar.clone(),
			       _eo,
			       (HashSet)((HashSet)touched_threads).clone());
    }

    
    /** Produces a <code>ParIntGraph</code> containing only the
	nodes that could be reached from the outside.
	(i.e. via parameters,
	class nodes, normally or exceptionally returned nodes or the
	started thread nodes) */
    public ParIntGraph keepTheEssential(PANode[] params, boolean is_main){
	ParIntGraph pig2 = retain_essential(params, is_main);

	if(AGGRESSIVE_SHRINKING){
	    pig2.shrinking();
	    pig2 = retain_essential(params, is_main);
	}

	return pig2;
    }

    private final ParIntGraph retain_essential(PANode[] params,
					       boolean is_main){
	HashSet remaining_nodes = new HashSet();
	remaining_nodes.addAll(tau.activeThreadSet());

	PointsToGraph _G = 
	    G.keepTheEssential(params, remaining_nodes, is_main);

	PAThreadMap _tau = (PAThreadMap) tau.clone();

	ActionRepository _ar = ar.keepTheEssential(remaining_nodes);

	EdgeOrdering _eo = 
	    PointerAnalysis.IGNORE_EO ? null : 
	    eo.keepTheEssential(remaining_nodes);

	// the "touched_threads" info is valid only for captured threads
	// i.e. not for the remaining nodes (accessible from the outside).
	return new ParIntGraph(_G, _tau, _ar, _eo, Collections.EMPTY_SET);
    }


    /** Aggressive shrinking: get rid of some unnecessary information.
	TODO: write some better comments. */
    private void shrinking() {
	final Set nodes = new HashSet(allNodes());
	final Set useful_nodes = new HashSet();
	final LinkedList Q = new LinkedList();
	
	init_queue(Q, useful_nodes, nodes);
	final Relation pred_rel = G.O.getPrecedenceRelation();

	while(!Q.isEmpty()) {
	    PANode node = (PANode) Q.removeFirst();
	    
	    for(Iterator it = pred_rel.getValues(node).iterator();
		it.hasNext(); ) {
		PANode pred = (PANode) it.next();
		if(useful_nodes.add(pred))
		    Q.addLast(pred);
	    }
	}

	System.out.println("as: " + nodes.size() + " -> " +
			   useful_nodes.size() + " delta=" +
			   (nodes.size() - useful_nodes.size()));

	// unuseful_nodes = nodes - useful_nodes
	nodes.removeAll(useful_nodes);

	remove(nodes);
    }
    

    private void init_queue(final LinkedList Q, final Set useful_nodes,
			    final Set nodes) {
	for(Iterator it = nodes.iterator(); it.hasNext(); ) {
	    PANode node = (PANode) it.next();
	    if(relevant_node(node))
		if(useful_nodes.add(node))
		    Q.addLast(node);
	}
	G.I.forAllEdges(new PAEdgeVisitor(){
		public void visit(Temp var, PANode node) {}
		public void visit(PANode node1, String f, PANode node2) {
		    if(useful_nodes.add(node1))
			Q.addLast(node1);
		    if(useful_nodes.add(node2))
			Q.addLast(node2);
		}
	    });
    }


    private boolean relevant_node(PANode node) {
	switch(node.type()){
	case PANode.INSIDE:
	    return true;
	case PANode.PARAM:
	    return true;
	case PANode.STATIC:
	    return true;
	case PANode.LOAD:
	    if(!G.e.escapesOnlyInCaller(node) ||
	       ar.isSyncOn(node) ||
	       G.r.contains(node) || G.excp.contains(node))
		return true;
	default:
	    if(G.willEscape(node))
		return true;
	}

	return false;
    }
    

    // Visits all the nodes from set_nodes.
    private void forSet(Set set_nodes, PANodeVisitor visitor){
	Iterator it_nodes = set_nodes.iterator();
	while(it_nodes.hasNext())
	    visitor.visit((PANode) it_nodes.next());
    }

    /** Visits all the nodes that appear in <code>this</code> graph. */
    public void forAllNodes(final PANodeVisitor visitor){
	G.O.forAllNodes(visitor);
	G.I.forAllNodes(visitor);
	forSet(G.r,visitor);
	forSet(G.excp,visitor);
	forSet(G.e.escapedNodes(),visitor);
	forSet(tau.activeThreadSet(),visitor);

	ar.forAllActions(new ActionVisitor(){
		public void visit_ld(PALoad load){
		    visitor.visit(load.n1);
		    visitor.visit(load.n2);
		    visitor.visit(load.nt);
		}
		public void visit_sync(PASync sync){
		    visitor.visit(sync.n);
		    visitor.visit(sync.nt);
		}
	    });

	ar.forAllParActions(new ParActionVisitor() {
		public void visit_par_ld(PALoad load, PANode nt2) {
		    visitor.visit(nt2);
		}
		public void visit_par_sync(PASync sync, PANode nt2) {
		    visitor.visit(nt2);
		}
	    });

	// the edges appearing in the edge ordering relation are also
	// present into G.O and G.I and so, they have been already visited
    }


    private Set all_nodes = null;

    /** Returns the set of all the nodes that appear in <code>this</code>
	parallel interaction graph.<br>
	<b>Warning:</b> This method should be called only on graphs that
	are final (ie they won't be modified through something as an edge
	addition, etc.). */
    public Set allNodes(){
	//	if(all_nodes == null){
	    final Set nodes = new HashSet();
	    forAllNodes(new PANodeVisitor(){
		    public void visit(PANode node){
			nodes.add(node);
		    }
		});
	    all_nodes = nodes;
	    //	}
	all_nodes.remove(ActionRepository.THIS_THREAD);
	return all_nodes;
    }

    /* Specialize <code>this</code> <code>ParIntGraph</code> for the call
       site <code>q</code>. */
    final ParIntGraph csSpecialize(final CALL call){
	/* contains mappings old node -> specialized node; each unmapped
	   node is supposed to be mapped to itself. */
	all_nodes = null;
	final Map map = new HashMap();
	for(Iterator itn = allNodes().iterator(); itn.hasNext(); ){
	    PANode node = (PANode) itn.next();
	    if(node.type == PANode.INSIDE)
		map.put(node, node.csSpecialize(call));
	} 

	//System.out.println("csSPECIALIZE -----------------");
	//System.out.println("Map: " + map);
	//System.out.println("AllNodes: " + allNodes());
	//System.out.println("OLD GRAPH: " + this);

	ParIntGraph new_graph = 
	    new ParIntGraph(G.specialize(map), tau.specialize(map), 
			    ar.csSpecialize(map, call),
			    PointerAnalysis.IGNORE_EO? null:eo.specialize(map),
			    PANode.specialize_set(touched_threads, map));

	//	System.out.println("NEW GRAPH: " + new_graph);
	//	System.out.println("-------------------------------");

	return new_graph;
    }

    /* Specializes <code>this</code> <code>ActionRepository</code> for the
       thread whose run method is <code>run</code>. */
    final ParIntGraph tSpecialize(final MetaMethod run){
	// contains mappings old node -> speciaized node; each unmapped
	// node is supposed to be mapped to itself.
	final Map map = new HashMap();
	for(Iterator itn = allNodes().iterator(); itn.hasNext(); ){
	    PANode node  = (PANode) itn.next();
	    if((node.type != PANode.PARAM) && (node.type != PANode.STATIC)){
		PANode node2 = node.tSpecialize(run);
		map.put(node, node2);
	    }
	}
	
	return
	    new ParIntGraph(G.specialize(map), tau.specialize(map), 
			    ar.tSpecialize(map, run), 
			    PointerAnalysis.IGNORE_EO? null: eo.specialize(map),
			    PANode.specialize_set(touched_threads, map));
    }

    // weak thread specialization
    final ParIntGraph wtSpecialize(final MetaMethod run){
	// contains mappings old node -> speciaized node; each unmapped
	// node is supposed to be mapped to itself.
	final Map map = new HashMap();
	for(Iterator itn = allNodes().iterator(); itn.hasNext(); ){
	    PANode node  = (PANode) itn.next();
	    if((node.type != PANode.PARAM) && (node.type != PANode.STATIC)){
		PANode node2 = node.wtSpecialize();
		map.put(node, node2);
	    }
	}
	
	return
	    new ParIntGraph(G.specialize(map), tau.specialize(map), 
			    ar.tSpecialize(map, run),
			    PointerAnalysis.IGNORE_EO?null:eo.specialize(map),
			    PANode.specialize_set(touched_threads, map));
    }


    /** Removes the nodes from <code>nodes</code> from <code>this</code>
	graph. */
    public void remove(Set nodes){
	G.remove(nodes);
	tau.remove(nodes);
	ar.removeNodes(nodes);

	if(!PointerAnalysis.IGNORE_EO)
	    eo.removeNodes(nodes);

	touched_threads.removeAll(nodes);
    }

    /** Simplify <code>this</code> parallel interaction graph by removing the
	loads that don't escape anywhere (and hence, don't represent any
	object). In addition, the <code>&lt;&lt;n1,f&gt;,n2&gt;</code>
	ouside (load) edges where <code>n1</code> is an unescaped node are
	removed too. */ 
    public void removeEmptyLoads(){
	final Set empty_loads = new HashSet();
	final Set fake_outside_edges = new HashSet();
	
	G.O.forAllEdges(new PAEdgeVisitor(){
		public void visit(Temp var, PANode node){}
		public void visit(PANode node1, String f, PANode node2){
		    if(!G.e.hasEscaped(node1))
			fake_outside_edges.add(new PAEdge(node1,f,node2));
		    if(!G.e.hasEscaped(node2))
			empty_loads.add(node2);
		}
	    });

	if(DEBUG){
	    System.out.println("Empty loads:" + empty_loads);
	    System.out.println("Fake outside edges: " + fake_outside_edges);
	}

	remove(empty_loads);

	Iterator it_edges = fake_outside_edges.iterator();
	while(it_edges.hasNext()){
	    PAEdge edge = (PAEdge) it_edges.next();
	    G.O.removeEdge(edge.n1,edge.f,edge.n2);
	}

	ar.removeEdges(fake_outside_edges);

	if(!PointerAnalysis.IGNORE_EO)
	    eo.removeEdges(fake_outside_edges);
    }

    /** Pretty-print function for debug purposes. 
	Two equal <code>ParIntGraph</code>s are guaranteed to have the same
	string representation. */
    public String toString(){
	return
	    "\nParIntGraph{\n" + G + " " + tau + 
	    " Touched threads: " + touchedToString() + "\n" +
	    ar + 
	    (PointerAnalysis.IGNORE_EO ? "" : eo.toString()) + 
	    "}";
    }

    // Produces a string representation of the
    // captured, started but touched threads.
    private String touchedToString(){
	StringBuffer buffer = new StringBuffer();

	Iterator it = touched_threads.iterator();
	while(it.hasNext()){
	    PANode nt = (PANode) it.next();
	    if(G.captured(nt))
		buffer.append(nt + " ");		
	}
	return buffer.toString();
    }


    /** Checks whether two <code>ParIntGraph</code>s are equal or not. In
	addition to the <code>equals</code> method, this handles the 
	comparisom of <code>null</code> objects. */
    public static boolean identical(ParIntGraph pig1, ParIntGraph pig2){
	if((pig1 == null) || (pig2 == null))
	    return (pig1 == pig2);
	return pig1.equals(pig2);
    }
}
