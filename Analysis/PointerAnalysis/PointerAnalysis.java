// PointerAnalysis.java, created Sat Jan  8 23:22:24 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.PointerAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;

import java.lang.reflect.Modifier;

import harpoon.ClassFile.HCodeFactory;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HMethod;
import harpoon.ClassFile.HField;
import harpoon.ClassFile.HCode;


import harpoon.Analysis.Quads.CallGraph;
import harpoon.Analysis.AllCallers;
import harpoon.Analysis.ClassHierarchy;
import harpoon.Util.LightBasicBlocks.LightBasicBlock;

import harpoon.Temp.Temp;

import harpoon.IR.Quads.Quad;
import harpoon.IR.Quads.QuadVisitor;
import harpoon.IR.Quads.HEADER;
import harpoon.IR.Quads.METHOD;
import harpoon.IR.Quads.AGET;
import harpoon.IR.Quads.ALENGTH;
import harpoon.IR.Quads.ANEW;
import harpoon.IR.Quads.ASET;
import harpoon.IR.Quads.GET;
import harpoon.IR.Quads.MOVE;
import harpoon.IR.Quads.NEW;
import harpoon.IR.Quads.RETURN;
import harpoon.IR.Quads.THROW;
import harpoon.IR.Quads.SET;
import harpoon.IR.Quads.CALL;
import harpoon.IR.Quads.MONITORENTER;
import harpoon.IR.Quads.MONITOREXIT;
import harpoon.IR.Quads.FOOTER;
import harpoon.IR.Quads.TYPECAST;

import harpoon.Analysis.MetaMethods.MetaMethod;
import harpoon.Analysis.MetaMethods.MetaCallGraph;
import harpoon.Analysis.MetaMethods.MetaAllCallers;

import harpoon.Util.TypeInference.CachingArrayInfo;

import harpoon.Util.LightBasicBlocks.LBBConverter;
import harpoon.Util.LightBasicBlocks.CachingSCCLBBFactory;
import harpoon.Util.Graphs.SCComponent;
import harpoon.Util.Graphs.SCCTopSortedGraph;
import harpoon.Util.UComp;

import harpoon.Util.Util;

/**
 * <code>PointerAnalysis</code> is the main class of the Pointer Analysis
 package. It is designed to act as a <i>query-object</i>: after being
 initialized, it can be asked to provide the Parallel InteractionGraph
 valid at the end of a specific method.
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: PointerAnalysis.java,v 1.1.2.59 2000-05-25 20:16:07 salcianu Exp $
 */
public class PointerAnalysis {
    public static final boolean DEBUG     = false;
    public static final boolean DEBUG2    = false;
    public static final boolean DEBUG_SCC = false;

    /** Makes the pointer analysis deterministic to make the debug easier.
	The main source of undeterminism in our code is the intensive use of
	<code>Set</code>s which doesn't offer any guarantee about the order
	in which they iterates over their elements. */
    public static final boolean DETERMINISTIC = true;

    /** Turns on the priniting of some timing info. */
    public static boolean TIMING = false;
    public static final boolean STATS = true;
    public static boolean SHOW_NODES = true;
    public static final boolean DETAILS2 = false;


    /** Hack to speed it up: it appears to me that the edge ordering
	relation is not extremely important: in recursive methods or in
	methods with loops, it tends to be just a cartesian product between
	I and O. */
    public static final boolean IGNORE_EO = true;

    /** Controls the recording of data about the thread nodes that are
	touched after being started. */
    public static final boolean TOUCHED_THREAD_SUPPORT= false;

    // TODO: Most of the following flags should be final (which will
    // help somehow the compiler to perform some dead code elimination
    // etc. and maybe gain some speed). For the moment, they are non-final
    // so that the main modeule can modify them (according to the command
    // line options).

    /** Activates the calling context sensitivity. When this flag is
	on, the nodes from the graph of the callee are specialized for each
	call site (up to <code>MAX_SPEC_DEPTH</code> times). This increases
	the precision of the analysis but requires more time and memory. */
    public static boolean CALL_CONTEXT_SENSITIVE = false;

    /** The specialization limit. This puts a limit to the otherwise
	exponential growth of the number of nodes in the analysis. */
    public static int MAX_SPEC_DEPTH = 1;

    /** Activates the full thread sensitivity. When this flag is on, 
	the analysis makes the distinction not only between the nodes
	allocated by the current thread and those allocated by all the
	others but also between the nodes allocated by threads with
	different run methods (for the time being, we cannot make the
	distinction between two threads with the same thread node). */
    public static boolean THREAD_SENSITIVE = false;

    /** Activates the weak thread sensitivity. When this flag is
	on, the precision of the interthread analysis is increased:
	the nodes from the graph of the run method of the thread whose
	interactions with the current thread are analyzed are specialized
	to differenciate between the nodes created by that thread and
	the nodes created by the current one. This increases
	the precision of the analysis but requires more time and memory. */
    public static boolean WEAKLY_THREAD_SENSITIVE = false;

    /** Activates the loop sensitivity. When this flag is on, the precision
	of the intra-method analysis is increased by making the difference
	between the last object allocated at a specific object creation
	site inside a loop and the objects allocated at the same object
	creation site but in the previous iterations. This enambles
	some strong optimizations but requires more time and memory. */
    public static boolean LOOP_SENSITIVE = false;

    // array elements are modeled as fields of the array object, all of them
    // with the same name since the analysis is not able to make the
    // distinction between the fields. 
    // this name is supposed to be "as impossible as possible", so that we
    // don't have any conflict with real fields.
    public static final String ARRAY_CONTENT = "+ae+";

    // The HCodeFactory providing the actual code of the analyzed methods
    private final MetaCallGraph  mcg;
    public final MetaCallGraph getMetaCallGraph() { return mcg; }
    private final MetaAllCallers mac;
    public final MetaAllCallers getMetaAllCallers() { return mac; }
    private final CachingSCCLBBFactory scc_lbb_factory;

    // Maintains the partial points-to and escape information for the
    // analyzed methods. This info is successively refined by the fixed
    // point algorithm until no further change is possible
    // mapping MetaMethod -> ParIntGraph.
    private Map hash_proc_int = new HashMap();

    // Maintains the external view of the parallel interaction graphs
    // attached with the analyzed methods. These "bricks" are used
    // for the processing of the CALL nodes.
    // Mapping MetaMethod -> ParIntGraph.
    private Map hash_proc_ext = new HashMap();

    /** Creates a <code>PointerAnalysis</code>.
     *<b>Parameters</b>
     *<ul>
     *<li>The <code>CallGraph</code> and the <code>AllCallers</code> that
     * models the relations between the different methods;
     *<li>A <code>BBConverter</code> that is used to generate the actual
     * code of the methods and
     *</ul> */
    public PointerAnalysis(MetaCallGraph _mcg, MetaAllCallers _mac,
			   LBBConverter lbbconv){
	mcg  = _mcg;
	mac  = _mac;
	scc_lbb_factory = new CachingSCCLBBFactory(lbbconv);
    }

    /** Returns the full (internal) <code>ParIntGraph</code> attached to
     * the method <code>hm</code> i.e. the graph at the end of the method.
     * Returns <code>null</code> if no such graph is available. */
    public ParIntGraph getIntParIntGraph(MetaMethod mm){
	ParIntGraph pig = (ParIntGraph)hash_proc_int.get(mm);
	if(pig == null){
	    analyze(mm);
	    pig = (ParIntGraph)hash_proc_int.get(mm);
	}
	return pig;
    }

    /** Returns the simplified (external) <code>ParIntGraph</code> attached to
     * the method <code>hm</code> i.e. the graph at the end of the method.
     * of which only the parts reachable from the exterior (via parameters,
     * returned objects or static classes) have been preserved. The escape
     * function do not consider the parameters of the function (anyway, this
     * graph is supposed to be inlined into the graph of the caller, so the 
     * parameters will disappear anyway).
     * Returns <code>null</code> if no such graph is available. */
    public ParIntGraph getExtParIntGraph(MetaMethod mm){
	return getExtParIntGraph(mm,true);
    }

    // internal method doing the job
    ParIntGraph getExtParIntGraph(MetaMethod mm, boolean compute_it){
	ParIntGraph pig = (ParIntGraph)hash_proc_ext.get(mm);
	if((pig == null) && compute_it){
	    analyze(mm);
	    pig = (ParIntGraph)hash_proc_ext.get(mm);
	}
	return pig;
    }

    // Returns a version of the external graph for meta-method hm that is
    // specialized for the call site q
    ParIntGraph getSpecializedExtParIntGraph(MetaMethod mm, CALL q){
	// first, search in the cache
	Map map_mm = (Map) cs_specs.get(mm);
	if(map_mm == null){
	    map_mm = new HashMap();
	    cs_specs.put(mm,map_mm);
	}
	ParIntGraph pig = (ParIntGraph) map_mm.get(q);
	if(pig != null) return pig;

	// if the specialization was not already in the cache,
	// try to recover the original ParIntGraph (if it exists) and
	// specialize it

	ParIntGraph original_pig = getExtParIntGraph(mm);
	if(original_pig == null) return null;

	ParIntGraph new_pig = original_pig.csSpecialize(q);
	map_mm.put(q,new_pig);

	return new_pig;	
    }
    // cache for call site sensitivity: Map<MetaMethod, Map<CALL,ParIntGraph>>
    private Map cs_specs = new HashMap();


    // Returns a specialized version of the external graph for meta-method mm;
    // mm is supposed to be the run method (the body) of a thread.
    ParIntGraph getSpecializedExtParIntGraph(MetaMethod mm){
	ParIntGraph pig = (ParIntGraph) t_specs.get(mm);
	if(pig != null) return pig;

	// if the specialization was not already in the cache,
	// try to recover the original ParIntGraph (if it exists) and
	// specialize it

	ParIntGraph original_pig = getExtParIntGraph(mm);
	if(original_pig == null) return null;

	ParIntGraph new_pig = null;
	if(THREAD_SENSITIVE)
	    new_pig = original_pig.tSpecialize(mm);
	else
	    if(WEAKLY_THREAD_SENSITIVE)
		new_pig = original_pig.wtSpecialize(mm);
	    else Util.assert(false,"The thread specialization is off!");

	t_specs.put(mm,new_pig);

	return new_pig;	
    }
    // cache for thread sensitivity: Map<MetaMethod, ParIntGraph>
    private Map t_specs = new HashMap();


    /** Returns the parameter nodes of the method <code>hm</code>. This is
     * useful for the understanding of the <code>ParIntGraph</code> attached
     * to <code>hm</code> */
    public PANode[] getParamNodes(MetaMethod mm){
	return nodes.getAllParams(mm);
    }

    /** Returns the parallel interaction graph for the end of the method 
	<code>hm</code>. The interactions between <code>hm</code> and the
	threads it (transitively) starts are analyzed in order to 
	&quot;recover&quot; some of the escaped nodes.<br>
	See Section 10 <i>Inter-thread Analysis</i> in the original paper of
	Martin and John Whaley for more details. */
    public ParIntGraph threadInteraction(MetaMethod mm){
	ParIntGraph pig = (ParIntGraph) getIntParIntGraph(mm);
	return InterThreadPA.resolve_threads(this, pig);
    }


    // Worklist of <code>MetaMethod</code>s for the inter-procedural analysis
    private PAWorkStack W_inter_proc = new PAWorkStack();

    // Worklist for the intra-procedural analysis; at any moment, it
    // contains only basic blocks from the same method.
    private PAWorkList  W_intra_proc = new PAWorkList();

    // Repository for node management.
    NodeRepository nodes = new NodeRepository(); 
    final NodeRepository getNodeRepository() { return nodes; }

    // Top-level procedure for the analysis. Receives the main method as
    // parameter. For the moment, it is not doing the inter-thread analysis
    private void analyze(MetaMethod mm){
	if(DEBUG)
	    System.out.println("ANALYZE: " + mm);

	// Navigator for the SCC building phase. The code is complicated
	// by the fact that we are interested only in yet unexplored methods
	// (i.e. whose parallel interaction graphs are not yet in the cache).
	SCComponent.Navigator navigator =
	    new SCComponent.Navigator(){
		    public Object[] next(Object node){
			MetaMethod[] mms  = mcg.getCallees((MetaMethod)node);
			MetaMethod[] mms2 = get_new_mmethods(mms);

			if(DETERMINISTIC)
			    Arrays.sort(mms2, UComp.uc);

			return mms2;
		    }

		    public Object[] prev(Object node){
			MetaMethod[] mms  = mac.getCallers((MetaMethod)node);
			MetaMethod[] mms2 = get_new_mmethods(mms);

			if(DETERMINISTIC)
			    Arrays.sort(mms2, UComp.uc);
			return mms2;
		    }

		    // selects only the (yet) unanalyzed methods
		    private MetaMethod[] get_new_mmethods(MetaMethod[] mms){
			int count = 0;
			boolean good[] = new boolean[mms.length];
			for(int i = 0 ; i < mms.length ; i++)
			    if(!hash_proc_ext.containsKey(mms[i])){
				good[i] = true;
				count++;
			    }
			    else
				good[i] = false;
			
			MetaMethod[] new_mms = new MetaMethod[count];
			int j = 0;
			for(int i = 0 ; i < mms.length ; i++)
			    if(good[i])
				new_mms[j++] = mms[i];
			return new_mms;
		    }
		};

	long begin_time = 0;
	begin_time = System.currentTimeMillis();

	if(DEBUG)
	  System.out.println("Creating the strongly connected components " +
			     "of methods ...");

	// SCComponent.DETERMINISTIC = DETERMINISTIC;

	// the topologically sorted graph of strongly connected components
	// composed of mutually recursive methods (the edges model the
	// caller-callee interaction).
	SCCTopSortedGraph mmethod_sccs = 
	    SCCTopSortedGraph.topSort(SCComponent.buildSCC(mm,navigator));


	if(DEBUG_SCC || TIMING){
	    long total_time = System.currentTimeMillis() - begin_time;
	    int counter = 0;
	    int mmethods = 0;
	    SCComponent scc = mmethod_sccs.getFirst();

	    if(DEBUG_SCC)
		System.out.println("===== SCCs of methods =====");

	    while(scc != null){
		if(DEBUG_SCC){
		    System.out.print(scc.toString(mcg));
		}
		counter++;
		mmethods += scc.nodeSet().size();
		scc = scc.nextTopSort();
	    }

	    if(DEBUG_SCC)
		System.out.println("===== END SCCs ============");

	    if(TIMING)
		System.out.println(counter + " component(s); " +
				   mmethods + " meta-method(s); " +
				   total_time + "ms processing time");
	}

	SCComponent scc = mmethod_sccs.getLast();
	while(scc != null){
	    analyze_inter_proc_scc(scc);
	    scc = scc.prevTopSort();
	}

	if(TIMING)
	    System.out.println("analyze(" + mm + ") finished in " +
			  (System.currentTimeMillis() - begin_time) + "ms");
    }

    // information about the interesting AGET quads
    CachingArrayInfo cai = new CachingArrayInfo();

    // inter-procedural analysis of a group of mutually recursive methods
    private void analyze_inter_proc_scc(SCComponent scc){

	if(TIMING || DEBUG){
	    System.out.print("SCC" + scc.getId() + 
			       "\t (" + scc.size() + " meta-method(s)){");
	    for(Iterator it = scc.nodes(); it.hasNext(); )
		System.out.print("\n " + it.next());
	    System.out.print("} ... ");
	}

	// Initially, the worklist (actually a workstack) contains only one
	// of the methods from the actual group of mutually recursive
	// methods. The others will be added later (because they are reachable
	// in the AllCaller graph from this initial node). 

	long begin_time = 0;
	if(TIMING) begin_time = System.currentTimeMillis();

	MetaMethod mmethod = null;
	if(DETERMINISTIC)
	    mmethod = (MetaMethod) scc.min();
	else
	    mmethod = (MetaMethod) scc.nodes().next();

	// if SCC composed of a native or abstract method, return immediately!
	if(!analyzable(mmethod.getHMethod())){
	    if(TIMING){
		long total_time = System.currentTimeMillis() - begin_time;
		System.out.println(total_time + "ms");
	    }
	    if(DEBUG)
		System.out.println(scc.toString(mcg) + " is unanalyzable");
	    return;
	}
	W_inter_proc.add(mmethod);

	boolean must_check = scc.isLoop();

	while(!W_inter_proc.isEmpty()){
	    // grab a method from the worklist
	    MetaMethod mm_work = (MetaMethod) W_inter_proc.remove();

	    ParIntGraph old_info = (ParIntGraph) hash_proc_ext.get(mm_work);

	    analyze_intra_proc(mm_work);
	    // clear the LightBasicBlock -> ParIntGraph data generated by
	    // analyze_intra_proc
	    //lbb2pig.clear(); // annotation
	    LBBConverter lbbconv = scc_lbb_factory.getLBBConverter();
	    LightBasicBlock.Factory lbbf = 
		lbbconv.convert2lbb(mm_work.getHMethod());
	    clear_lbb2pig(lbbf);
	    //end annotation

	    ParIntGraph new_info = (ParIntGraph) hash_proc_ext.get(mm_work);

	    //ParIntGraph.DEBUG2 = true;

	    // new info?
	    if(must_check && !new_info.equals(old_info)){

		// System.out.println("----- The information has changed:");
		// System.out.println("Old graph: " + old_info);
		// System.out.println("New graph: " + new_info);

		// since the original graph associated with hm_work changed,
		// the old specializations for it are no longer actual;
		if(CALL_CONTEXT_SENSITIVE)
		    cs_specs.remove(mm_work);

		Object[] mms = mac.getCallers(mm_work);    

		if(DETERMINISTIC){
		    Arrays.sort(mms,UComp.uc);
		}
		for(int i = 0; i < mms.length ; i++){
		    MetaMethod mm_caller = (MetaMethod) mms[i];
		    if(scc.contains(mm_caller))
			W_inter_proc.add(mm_caller);
		}
	    }

	    //ParIntGraph.DEBUG2 = false;
	}

	// enable some GC:
	// 1. get rid of the cache from the scc_lbb factory; anyway, it is
	// unsuseful, since the methods from scc are never reanalyzed. 
	// 1b. the info attached to the LBBs will be collected by the GCC too
	scc_lbb_factory.clear();
	// 2. the meta methods of this SCC will never be revisited, so the
	// specializations generated for the call sites inside them are not
	// usefull any more
	if(CALL_CONTEXT_SENSITIVE)
	    cs_specs.clear();
	// 3. clear the array info
	cai.clear();

	long total_time = 0;
	if(TIMING)
	    total_time = System.currentTimeMillis() - begin_time;

	if(TIMING)
	    System.out.println(total_time + "ms");
    }

    private MetaMethod current_intra_mmethod = null;
    private ParIntGraph initial_pig = null;

    // the set of the AGETs from arrays of non-primitive types.
    private Set good_agets = null;

    // Performs the intra-procedural pointer analysis.
    private void analyze_intra_proc(MetaMethod mm){
	if(DEBUG)
	    System.out.println("META METHOD: " + mm);

	if(STATS) Stats.record_mmethod_pass(mm);

	LBBConverter lbbconv = scc_lbb_factory.getLBBConverter();
	LightBasicBlock.Factory lbbf = lbbconv.convert2lbb(mm.getHMethod());
	HCode hcode = lbbf.getHCode();
	good_agets = cai.getInterestingAGETs(mm.getHMethod(), hcode);

	current_intra_mmethod = mm;

	// cut the method into SCCs of basic blocks
	SCComponent scc = 
	    scc_lbb_factory.computeSCCLBB(mm.getHMethod()).getFirst();

	if(DEBUG2){
	    System.out.println("THE CODE FOR :" + mm.getHMethod());
	    Debug.show_lbb_scc(scc);
	}

	if(STATS) Stats.record_mmethod(mm,scc);

	// construct the ParIntGraph at the beginning of the method 
	LightBasicBlock first_bb = (LightBasicBlock)scc.nodes().next();
	HEADER first_hce = (HEADER) first_bb.getElements()[0];
	METHOD m  = (METHOD) first_hce.next(1);
	initial_pig = get_mmethod_initial_pig(mm,m);

	// analyze the SCCs in decreasing topological order
	while(scc != null){
	    analyze_intra_proc_scc(scc);
	    scc = scc.nextTopSort();
	}

	good_agets = null;
    }

    // Intra-procedural analysis of a strongly connected component of
    // basic blocks.
    private void analyze_intra_proc_scc(SCComponent scc){

	if(DEBUG2)
	    System.out.println("\nSCC" + scc.getId());

	// add ALL the BBs from this SCC to the worklist.
	if(DETERMINISTIC){
	    Object[] obj = Debug.sortedSet(scc.nodeSet());
	    for(int i = 0; i < obj.length; i++)
		W_intra_proc.add(obj[i]);
	}
	else
	    W_intra_proc.addAll(scc.nodeSet());

	boolean must_check = scc.isLoop();

	while(!W_intra_proc.isEmpty()){
	    // grab a Basic Block from the worklist
	    LightBasicBlock lbb_work = (LightBasicBlock) W_intra_proc.remove();

	    //ParIntGraph old_info = (ParIntGraph) lbb2pig.get(lbb_work);
	    // annotation
	    ParIntGraphPair old_info = (ParIntGraphPair) lbb_work.user_info;
	    analyze_basic_block(lbb_work);
	    ParIntGraphPair new_info = (ParIntGraphPair) lbb_work.user_info;

	    if(must_check && !ParIntGraphPair.identical(old_info, new_info)){
		// yes! The succesors of the analyzed basic block
		// are potentially "interesting", so they should be added
		// to the intra-procedural worklist

		LightBasicBlock[] next_lbbs = lbb_work.getNextLBBs();
		int len = next_lbbs.length;
		for(int i = 0; i < len; i++){
		    LightBasicBlock lbb_next = next_lbbs[i];
		    if(scc.contains(lbb_next))
			W_intra_proc.add(lbb_next);
		}
	    }
	}

    }


    /** The Parallel Interaction Graph which is updated by the
     *  <code>analyze_basic_block</code>. This should normally be a
     *  local variable of that function but it must be also accessible
     *  to the <code>PAVisitor</code> class */
    private ParIntGraph lbbpig = null;

    // The pair of ParIntGraphs computed by the inter-procedural analysis
    // will be put here. 
    private ParIntGraphPair call_pp = null;

    /** QuadVisitor for the <code>analyze_basic_block</code> */
    private class PAVisitor extends QuadVisitor{

	/** Visit a general quad, other than those explicitly
	 processed by the other <code>visit</code> methods. */
	public void visit(Quad q){
	    // remove all the edges from the temps that are defined
	    // by this quad.
	    Temp[] defs = q.def();
	    for(int i = 0; i < defs.length; i++)
		lbbpig.G.I.removeEdges(defs[i]);
	}
		

	public void visit(HEADER q){
	    // do nothing
	}

	public void visit(METHOD q){
	    // do nothing
	}

	public void visit(TYPECAST q){
	    // do nothing
	}

	/** Copy statements **/
	public void visit(MOVE q){
	    lbbpig.G.I.removeEdges(q.dst());
	    Set set = lbbpig.G.I.pointedNodes(q.src());
	    lbbpig.G.I.addEdges(q.dst(),set);
	}
	
	
	// LOAD STATEMENTS
	/** Load statement; normal case */
	public void visit(GET q){
	    Temp l2 = q.objectref();
	    HField hf = q.field();

	    // do not analyze loads from non-pointer fields
	    if(hf.getType().isPrimitive()) return;

	    if(l2 == null){
		// special treatement of the static fields
		l2 = ArtificialTempFactory.getTempFor(hf);
		// this part should really be put in some preliminary step
		PANode static_node =
		    nodes.getStaticNode(hf.getDeclaringClass().getName());
		lbbpig.G.I.addEdge(l2,static_node);
		lbbpig.G.e.addNodeHole(static_node,static_node);
	    }
	    process_load(q,q.dst(),l2,hf.getName());
	}
	
	/** Load statement; special case - arrays. */
	public void visit(AGET q){
	    if(DEBUG2){
		System.out.println("AGET: " + q);
		System.out.println("good_agets: " + good_agets);
	    }

	    // AGET from an array of primitive objects (int, float, ...)
	    if(!good_agets.contains(q)){
		if(DEBUG)
		    System.out.println("NOT-PA RELEVANT AGET: " + 
				       q.getSourceFile() + ":" + 
				       q.getLineNumber() + " " + q);
		// not interesting for the pointer analysis
		lbbpig.G.I.removeEdges(q.dst());
		return;
	    }
	    // All the elements of an array are collapsed in a single
	    // node, referenced through a conventional named field
	    process_load(q, q.dst(), q.objectref(), ARRAY_CONTENT);
	}
	
	/** Does the real processing of a load statement. */
	public void process_load(Quad q, Temp l1, Temp l2, String f){
	    Set set_aux = lbbpig.G.I.pointedNodes(l2);
	    Set set_S = lbbpig.G.I.pointedNodes(set_aux,f);
	    HashSet set_E = new HashSet();
	    
	    Iterator it = set_aux.iterator();
	    while(it.hasNext()){
		PANode node = (PANode)it.next();
		// hasEscaped instead of escaped (there is no problem
		// with the nodes that *will* escape - the future cannot
		// affect us).
		if(lbbpig.G.e.hasEscaped(node))
		    set_E.add(node);
	    }
	    
	    lbbpig.G.I.removeEdges(l1);
	    
	    if(set_E.isEmpty()){
		lbbpig.G.I.addEdges(l1,set_S);
	    }
	    else{
		PANode load_node = nodes.getCodeNode(q,PANode.LOAD); 
		set_S.add(load_node);
		lbbpig.G.O.addEdges(set_E,f,load_node);

		if(!PointerAnalysis.IGNORE_EO)
		    lbbpig.eo.add(set_E, f, load_node, lbbpig.G.I);

		lbbpig.G.I.addEdges(l1,set_S);
		lbbpig.G.propagate(set_E);

		// update the action repository
		Set active_threads = lbbpig.tau.activeThreadSet();
		Iterator it_esc_nodes = set_E.iterator();

		while(it_esc_nodes.hasNext()){
		    PANode ne = (PANode) it_esc_nodes.next();
		    lbbpig.ar.add_ld(ne, f, load_node,
				    ActionRepository.THIS_THREAD,
				    active_threads);
		}
	    }

	    if(TOUCHED_THREAD_SUPPORT) touch_threads(set_aux);
	}


	// OBJECT CREATION SITES
	/** Object creation sites; normal case */
	public void visit(NEW q){
	    process_new(q,q.dst());
	}
	
	/** Object creation sites; special case - arrays */
	public void visit(ANEW q){
	    process_new(q,q.dst());
	}
	
	private void process_new(Quad q,Temp tmp){
	    // Kill_I = edges(I,l)
	    PANode node = nodes.getCodeNode(q,PANode.INSIDE);
	    lbbpig.G.I.removeEdges(tmp);
	    // Gen_I = {<l,n>}
	    lbbpig.G.I.addEdge(tmp,node);
	}
	
	
	/** Return statement: r' = I(l) */
	public void visit(RETURN q){
	    Temp tmp = q.retval();
	    // return without value; nothing to be done
	    if(tmp == null) return;
	    Set set = lbbpig.G.I.pointedNodes(tmp);
	    // TAKE CARE: r is assumed to be empty before!
	    lbbpig.G.r.addAll(set);
	}

	/** Return statement: r' = I(l) */
	public void visit(THROW q){
	    Temp tmp = q.throwable();
	    Set set = lbbpig.G.I.pointedNodes(tmp);
	    // TAKE CARE: excp is assumed to be empty before!
	    lbbpig.G.excp.addAll(set);
	}
	
	
	// STORE STATEMENTS
	/** Store statements; normal case */
	public void visit(SET q){
	    Temp   l1 = q.objectref();
	    HField hf = q.field();
	    // do not analyze stores into non-pointer fields
	    if(hf.getType().isPrimitive()) return;
	    // static field -> get the corresponding artificial node
	    if(l1 == null){
		// special treatement of the static fields
		l1 = ArtificialTempFactory.getTempFor(hf);
		// this part should really be put in some preliminary step
		PANode static_node =
		    nodes.getStaticNode(hf.getDeclaringClass().getName());
		lbbpig.G.I.addEdge(l1,static_node);
		lbbpig.G.e.addNodeHole(static_node,static_node);
	    }
	    process_store(l1,hf.getName(),q.src());
	}
	
	/** Store statement; special case - array */
	public void visit(ASET q){
	    // All the elements of an array are collapsed in a single
	    // node
	    process_store(q.objectref(), ARRAY_CONTENT, q.src());
	}
	
	/** Does the real processing of a store statement */
	public void process_store(Temp l1, String f, Temp l2){
	    Set set1 = lbbpig.G.I.pointedNodes(l1);
	    Set set2 = lbbpig.G.I.pointedNodes(l2);
		
	    lbbpig.G.I.addEdges(set1,f,set2);
	    lbbpig.G.propagate(set1);

	    if(TOUCHED_THREAD_SUPPORT) touch_threads(set1);
	}
	
	public void process_thread_start_site(CALL q){
	    if(DEBUG2)
		System.out.println("THREAD START SITE: " + 
				   q.getSourceFile() + ":" +
				   q.getLineNumber());

	    // the parallel interaction graph in the case no thread
	    // is started due to some error and an exception is returned
	    // back from the "start()"
	    ParIntGraph lbbpig_no_thread = (ParIntGraph) (lbbpig.clone());

	    Temp l = q.params(0);
	    Set set = lbbpig.G.I.pointedNodes(l);
	    lbbpig.tau.incAll(set);
	    
	    Iterator it_nt = set.iterator();
	    while(it_nt.hasNext()){
		PANode nt = (PANode) it_nt.next();
		lbbpig.G.e.addNodeHole(nt,nt);
		lbbpig.G.propagate(Collections.singleton(nt));
	    }

	    call_pp = new ParIntGraphPair(lbbpig, lbbpig_no_thread);	    
	}

	public void visit(CALL q){
	    // treat the thread start sites specially
	    if(thread_start_site(q)){
		process_thread_start_site(q);
		return;
	    }

	    call_pp = 
		InterProcPA.analyze_call(PointerAnalysis.this,
					 current_intra_mmethod,
					 q,       // the CALL site
					 lbbpig); // the graph before the call
	}

	// We are sure that the call site q corresponds to a thread start
	// site if only java.lang.Thread.start can be called there. (If
	// some other methods can be called, it is possible that some of
	// them do not start a thread.)
	private boolean thread_start_site(CALL q){
	    MetaMethod mms[] = mcg.getCallees(current_intra_mmethod,q);
	    if(mms.length!=1) return false;

	    HMethod hm = mms[0].getHMethod();
	    String name = hm.getName();
	    if((name==null) || !name.equals("start")) return false;

	    if(hm.isStatic()) return false;
	    HClass hclass = hm.getDeclaringClass();
	    return hclass.getName().equals("java.lang.Thread");
	}

	/** Process an acquire statement. */
	public void visit(MONITORENTER q){
	    process_acquire_release(q, q.lock());
	}

	/** Process a release statement. */
	public void visit(MONITOREXIT q){
	    process_acquire_release(q, q.lock());
	}

	// Does the real processing for acquire/release statements.
	private void process_acquire_release(Quad q, Temp l){
	    Set active_threads = lbbpig.tau.activeThreadSet();
	    Iterator it_nodes = lbbpig.G.I.pointedNodes(l).iterator();
	    while(it_nodes.hasNext()){
		PANode node = (PANode) it_nodes.next();
		PASync sync = new PASync(node,ActionRepository.THIS_THREAD, q);
		lbbpig.ar.add_sync(sync, active_threads);
	    }

	    if(TOUCHED_THREAD_SUPPORT)
		touch_threads(lbbpig.G.I.pointedNodes(l));
	}

	// Records the fact that the started thread from the set 
	// "set_touched_nodes" have been touched.
	private void touch_threads(Set set_touched_nodes){
	    for(Iterator it = set_touched_nodes.iterator(); it.hasNext();){
		PANode touched_node = (PANode)it.next();
		if((touched_node.type == PANode.INSIDE) &&
		   lbbpig.tau.isStarted(touched_node))
		    lbbpig.touch_thread(touched_node);
	    }
	}


	/** End of the currently analyzed method; store the graph
	    in the hash table. */
	public void visit(FOOTER q){
	    // The full graph is stored in the hash_proc_int hashtable;
	    hash_proc_int.put(current_intra_mmethod,lbbpig);
	    // To obtain the external view of the method, the graph must be
	    // shrinked to the really necessary parts: only the stuff
	    // that is accessible from the "root" nodes (i.e. the nodes
	    // that can be accessed by the rest of the program - e.g.
	    // the caller).
	    // The set of root nodes consists of the param and return
	    // nodes, and (for the non-"main" methods) the static nodes.
	    // TODO: think about the static nodes vs. "main"
	    PANode[] nodes = getParamNodes(current_intra_mmethod);
	    boolean is_main = 
		current_intra_mmethod.getHMethod().getName().equals("main");
	    ParIntGraph shrinked_graph =lbbpig.keepTheEssential(nodes,is_main);

	    // We try to correct some imprecisions in the analysis:
	    //  1. if the meta-method is not returning an Object, clear
	    // the return set of the shrinked_graph
	    HMethod hm = current_intra_mmethod.getHMethod();
	    if(hm.getReturnType().isPrimitive())
		shrinked_graph.G.r.clear();
	    //  2. if the meta-method doesn't throw any exception,
	    // clear the exception set of the shrinked_graph.
	    if(hm.getExceptionTypes().length == 0)
		shrinked_graph.G.excp.clear();

	    // The external view of the graph is stored in the
	    // hash_proc_ext hashtable;
	    hash_proc_ext.put(current_intra_mmethod,shrinked_graph);
	}
	
    }
    

    // Quad Visitor used by analayze_basic_block
    private PAVisitor pa_visitor = new PAVisitor();
    
    /** Analyzes a basic block - a Parallel Interaction Graph is computed at
     *  the beginning of the basic block, it is next updated by all the 
     *  instructions appearing in the basic block (in the order they appear
     *  in the original program). */
    private void analyze_basic_block(LightBasicBlock lbb){
	if(DEBUG2){
	    System.out.println("BEGIN: Analyze_basic_block " + lbb);
	    System.out.print("Prev BBs: ");
	    Object[] prev_lbbs = lbb.getPrevLBBs();
	    Arrays.sort(prev_lbbs, UComp.uc);
	    for(int i = 0 ; i < prev_lbbs.length ; i++)
		System.out.print((LightBasicBlock) prev_lbbs[i] + " ");
	    System.out.println();
	}

	// lbbpig is the graph at the *bb point; it will be 
	// updated till it becomes the graph at the bb* point
	lbbpig = get_initial_bb_pig(lbb);

	if(DEBUG2){
	    System.out.println("Before:");
	    System.out.println(lbbpig);
	}

	// go through all the instructions of this basic block
	HCodeElement[] instrs = lbb.getElements();
	int len = instrs.length;
	for(int i = 0; i < len; i++){
	    Quad q = (Quad) instrs[i];

	    if(DEBUG2)
		System.out.println("INSTR: " + q.getSourceFile() + ":" +
				   q.getLineNumber() + " " + q);
	    
	    // update the Parallel Interaction Graph according
	    // to the current instruction
	    q.accept(pa_visitor);
	}

	// if there was a pair, store the pair computed by the inter proc
	// module, otherwise, only the first element of the pair is essential.
	if(call_pp != null){
	    lbb.user_info = call_pp;
	    call_pp = null;
	}
	else
	    lbb.user_info = new ParIntGraphPair(lbbpig, null);

	if(DEBUG2){
	    System.out.println("After:");
	    System.out.println(lbbpig);
	    System.out.print("Next BBs: ");
	    Object[] next_lbbs = lbb.getNextLBBs();
	    Arrays.sort(next_lbbs, UComp.uc);
	    for(int i = 0 ; i < next_lbbs.length ; i++)
		System.out.print((LightBasicBlock) next_lbbs[i] + " ");
	    System.out.println("\n");
	}
    }
    
    /** Returns the Parallel Interaction Graph at the point bb*
     *  The returned <code>ParIntGraph</code> must not be modified 
     *  by the caller. This function is used by 
     *  <code>get_initial_bb_pig</code>. */
    private ParIntGraph get_after_bb_pig(LightBasicBlock lbb,
					 LightBasicBlock lbb_son){

	ParIntGraphPair pp = (ParIntGraphPair) lbb.user_info;
	if(pp == null)
	    return ParIntGraph.EMPTY_GRAPH;

	int k = 0;
	HCodeElement last_lbb = lbb.getLastElement();
	if((last_lbb != null) && (last_lbb instanceof CALL)){
	    CALL call = (CALL) last_lbb;
	    HCodeElement first_lbb_son = lbb_son.getFirstElement();
	    if(!call.next(0).equals(first_lbb_son)) k = 1;
	}

	//ParIntGraph pig = (ParIntGraph) lbb2pig.get(lbb);
	ParIntGraph pig = pp.pig[k];
	return (pig==null)?ParIntGraph.EMPTY_GRAPH:pig;
    }


    /** (Re)computes the Parallel Interaction Thread associated with
     *  the beginning of the <code>LightBasicBlock</code> <code>lbb</code>.
     *  This method is recomputing the stuff (instead of just grabbing it
     *  from the cache) because the information attached with some of
     *  the predecessors has changed (that's why <code>bb</code> is 
     *  reanalyzed) */
    private ParIntGraph get_initial_bb_pig(LightBasicBlock lbb){
	LightBasicBlock[] prev = lbb.getPrevLBBs();
	int len = prev.length;
	if(len == 0){
	    // This case is treated specially, it's about the
	    // graph at the beginning of the current method.
	    ParIntGraph pig = initial_pig;
	    return pig;
	}
	else{
	    // do the union of the <code>ParIntGraph</code>s attached to
	    // all the predecessors of this basic block
	    ParIntGraph pig = 
		(ParIntGraph) (get_after_bb_pig(prev[0], lbb)).clone();

	    for(int i = 1; i < len; i++)
		pig.join(get_after_bb_pig(prev[i], lbb));

	    return pig;
	}
    }


    /** Computes the parallel interaction graph at the beginning of a 
     *  method; an almost empty graph except for the parameter nodes
     *  for the object formal parameter (i.e. primitive type parameters
     *  such as <code>int</code>, <code>float</code> do not have associated
     *  nodes */
    private ParIntGraph get_mmethod_initial_pig(MetaMethod mm, METHOD m){
	Temp[]  params = m.params();
	HMethod     hm = mm.getHMethod();
	HClass[] types = hm.getParameterTypes();

	ParIntGraph pig = new ParIntGraph();

	// the following code is quite messy ... The problem is that I 
	// create param nodes only for the parameter with object types;
	// unfortunately, the types could be found only in HMethod (and
	// do not include the evetual this parameter for non-static nodes)
	// while the actual Temps associated with all the formal parameters
	// could be found only in METHOD. So, we have to coordinate
	// information from two different places and, even more, we have
	// to handle the missing this parameter (which is present in METHOD
	// but not in HMethod). 
	boolean isStatic = 
	    java.lang.reflect.Modifier.isStatic(hm.getModifiers());
	// if the method is non-static, the first parameter is not metioned
	// in HMethod - it's the implicit this parameter.
	int skew = isStatic?0:1;
	// number of object formal parameters = the number of param nodes
	int count = skew;
	for(int i = 0; i < types.length; i++)
	    if(!types[i].isPrimitive()) count++;
	
	nodes.addParamNodes(mm,count);
	Stats.record_mmethod_params(mm,count);

	// add all the edges of type <p,np> (i.e. parameter to 
	// parameter node) - just for the non-primitive types (e.g. int params
	// do not clutter our analysis)
	// the edges for the static fields will
	// be added later.
	count = 0;
	for(int i = 0; i < params.length; i++)
	    if((i<skew) || ((i>=skew) && !types[i-skew].isPrimitive())){
		PANode param_node = nodes.getParamNode(mm,count);
		pig.G.I.addEdge(params[i],param_node);
		// The param nodes are escaping through themselves */
		pig.G.e.addNodeHole(param_node,param_node);
		count++;
	    }

	return pig;
    }

    /** Check if <code>hm</code> can be analyzed by the pointer analysis. */
    public static final boolean analyzable(HMethod hm){
	int modifier = hm.getModifiers();
	return ! (
	    (java.lang.reflect.Modifier.isNative(modifier)) ||
	    (java.lang.reflect.Modifier.isAbstract(modifier))
	    );
    }


    // the set of harmful native methods
    private static Set hns = new HashSet();
    //    static{
    // here should be put some initializations for the hns set
    //}

    public final boolean harmful_native(HMethod hm){
	if(!Modifier.isNative(hm.getModifiers()))
	    return false;
	return hns.contains(hm);
    }

    /** Prints some statistics. */
    public final void  print_stats(){
	if(!STATS){
	    System.out.println("Statistics are deactivated.");
	    System.out.println("Turn on the PointerAnalysis.STATS flag!");
	    return;
	}

	Stats.print_stats();
	nodes.print_stats();
	System.out.println("==========================================");
	if(SHOW_NODES){
	    System.out.println("BASIC NODES");
	    System.out.println(nodes);
	    System.out.println("NODE SPECIALIZATIONS:");
	    nodes.show_specializations();
	}
    }

    /** Returns the parallel interaction graph valid at the program point
	right before <code>q</code>. <code>q</code> belongs to the light basic
	block <code>lbb</code> of the <code>current_intra_mmethod</code>.
	The analysis is re-executed from the beginning of <code>lbb</code>,
	till we reach <code>q</code> when we stop it and return the 
	parallel intercation graph valid at that moment. */
    private ParIntGraph analyze_lbb_up_to_q(LightBasicBlock lbb, Quad q){
	lbbpig = get_initial_bb_pig(lbb);

	HCodeElement[] instrs = lbb.getElements();
	for(int i = 0; i < instrs.length; i++){
	    Quad q_curr = (Quad) instrs[i];
	    if(q_curr.equals(q)) return lbbpig;
	    q_curr.accept(pa_visitor);
	}
	Util.assert(false, q + " was not in " + lbb);
	return null; // this should never happen
    }
    

    /** Returns the parallel interaction graph attached to the program point
	right before <code>q</code> in the body of meta-method
	<code>mm</code>. */
    public final ParIntGraph getPIGAtQuad(MetaMethod mm, Quad q){
	Util.assert(mcg.getAllMetaMethods().contains(mm),
		    "Uncalled/unknown meta-method!");
	LBBConverter lbbconv = scc_lbb_factory.getLBBConverter();
	LightBasicBlock.Factory lbbf = lbbconv.convert2lbb(mm.getHMethod());
	Util.assert(lbbf != null, "Fatal error");
	LightBasicBlock lbb = lbbf.getBlock(q);
	Util.assert(lbb != null, "No (Light)BasicBlock found for " + q);
	// as all the ParIntGraph's for methods are computed, a simple
	// intra-procedural analysis of mm (with some caller-callee
	// interactions) is enough.
	analyze_intra_proc(mm);
	// now, we found the right basic block, we also have the results
	// for all the basic block of the concerned method; all we need
	// is to redo the pointer analysis for that basic block, stopping
	// when we meet q.
	ParIntGraph retval = analyze_lbb_up_to_q(lbb,q);
	// clear the LightBasicBlock -> ParIntGraph cache generated by
	// analyze_intra_proc

	//lbb2pig.clear();
	clear_lbb2pig(lbbf); // annotation;

	return retval;
    }

    // activates the GC
    private final void clear_lbb2pig(LightBasicBlock.Factory lbbf){
	LightBasicBlock[] lbbs = lbbf.getAllBBs();
	for(int i = 0; i < lbbs.length; i++)
	    lbbs[i].user_info = null;
    }

    // clears all the info attached to LBBs belonging to the code of
    // methods from the strongly connected component scc.
    private final void clear_lbb_info(SCComponent scc){
	LBBConverter lbbconv = scc_lbb_factory.getLBBConverter();

	for(Iterator it = scc.nodes(); it.hasNext(); ){
	    MetaMethod mm_work = (MetaMethod) it.next();
	    LightBasicBlock.Factory lbbf = 
		lbbconv.convert2lbb(mm_work.getHMethod());
	    clear_lbb2pig(lbbf);
	}
    }

    /** Returns the set of the nodes pointed by the temporary <code>t</code>
	at the point right before executing instruction <code>q</code>
	from the body of meta-method <code>mm</code>. */
    public final Set pointedNodes(MetaMethod mm, Quad q, Temp l){
	// 1. obtain the Parallel Interaction Graph attached to the point
	// right before the execution of q (noted .q in the article). 
	ParIntGraph pig = getPIGAtQuad(mm,q);
	// 2. look at the nodes pointed by l; as from a temporary we can
	// have only inside edges, it is enough to look in the I set.
	return pig.G.I.pointedNodes(l);
    }


    /* 
    private ParIntGraph get_mmethod_initial_pig(MetaMethod mm, METHOD m){
	Temp[]  params = m.params();
	HMethod     hm = mm.getHMethod();
	HClass[] types = hm.getParameterTypes();

	ParIntGraph pig = new ParIntGraph();

	// the following code is quite messy ... The problem is that I 
	// create param nodes only for the parameter with object types;
	// unfortunately, the types could be found only in HMethod (and
	// do not include the evetual this parameter for non-static nodes)
	// while the actual Temps associated with all the formal parameters
	// could be found only in METHOD. So, we have to coordinate
	// information from two different places and, even more, we have
	// to handle the missing this parameter (which is present in METHOD
	// but not in HMethod). 
	boolean isStatic = 
	    java.lang.reflect.Modifier.isStatic(hm.getModifiers());
	// if the method is non-static, the first parameter is not metioned
	// in HMethod - it's the implicit this parameter.
	int skew = isStatic?0:1;
	// number of object formal parameters = the number of param nodes
	int count = skew;
	for(int i = 0; i < types.length; i++)
	if(!types[i].isPrimitive()) count++;
	
	nodes.addParamNodes(mm,count);
	Stats.record_mmethod_params(mm,count);

	// add all the edges of type <p,np> (i.e. parameter to 
	// parameter node) - just for the non-primitive types (e.g. int params
	// do not clutter our analysis)
	// the edges for the static fields will
	// be added later.
	count = 0;
	for(int i = 0; i < params.length; i++)
	    if((i<skew) || ((i>=skew) && !types[i-skew].isPrimitive())){
		PANode param_node = nodes.getParamNode(mm,count);
		pig.G.I.addEdge(params[i],param_node);
		// The param nodes are escaping through themselves
		pig.G.e.addNodeHole(param_node,param_node);
		count++;
	    }

	return pig;
    }
    */

    /*
    ////////// SPECIAL HANDLING FOR SOME NATIVE METHODS ////////////////////

    final ParIntGraph getExpParIntGraph(MetaMethod mm){
	HMethod hm = mm.getHMethod();

	ParIntGraph pig = (ParIntGraph) hash_proc_ext.get(mm);
	if(pig == null){
	    pig = ext_pig_for_native(hm);
	    if(pig != null) hash_proc_ext.put(mm, pig);
	}

	return pig;
    }

    private final ParIntGraph ext_pig_for_native(HMethod hm){
	HClass hclass = hm.getDeclaringClass();
	String clname = hclass.getName();
	String hmname = hm.getName();

	if(clname.equals("java.lang.Object") &&
	   hmname.equals("hashCode"));
	    
    }
    */

}


