// MetaCallGraphImpl.java, created Wed Mar  8 15:20:29 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.MetaMethods;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Collections;

import java.lang.reflect.Modifier;

import harpoon.Analysis.ClassHierarchy;
import harpoon.Analysis.ReachingDefs;
import harpoon.Analysis.ReachingDefsImpl;
import harpoon.Analysis.BasicBlock;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HMethod;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.Linker;
import harpoon.ClassFile.Loader;

import harpoon.IR.Quads.QuadVisitor;
import harpoon.IR.Quads.Quad;
import harpoon.IR.Quads.HEADER;
import harpoon.IR.Quads.METHOD;
import harpoon.IR.Quads.CALL;
import harpoon.IR.Quads.MOVE;
import harpoon.IR.Quads.GET;
import harpoon.IR.Quads.AGET;
import harpoon.IR.Quads.NEW;
import harpoon.IR.Quads.ANEW;
import harpoon.IR.Quads.TYPECAST;
import harpoon.IR.Quads.CONST;

import harpoon.Temp.Temp;

import harpoon.Util.Graphs.SCComponent;
import harpoon.Util.Graphs.SCCTopSortedGraph;
import harpoon.Util.BasicBlocks.CachingBBConverter;
import harpoon.Analysis.PointerAnalysis.PAWorkList;

import harpoon.Util.Util;

import harpoon.Util.DataStructs.Relation;
import harpoon.Util.DataStructs.LightRelation;
import harpoon.Util.DataStructs.RelationEntryVisitor;


/**
 * <code>MetaCallGraphImpl</code> is a full-power implementation of the
 <code>MetaCallGraph</code> interface. This is <i>the</i> interface to use
 if you want to play with meta-methods. <br>

 Otherwise, you can simply use
 <code>FakeCallGraph</code> which allows you to run things that need
 meta method representation of the program even without generating them
 by simply providing a meta methods-like interface for the standard
 <code>CallGraph</code>.
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: MetaCallGraphImpl.java,v 1.1.2.21 2000-07-02 08:37:40 salcianu Exp $
 */
public class MetaCallGraphImpl extends MetaCallGraphAbstr {

    private static boolean DEBUG = false;
    private static boolean DEBUG_CH = false;
    private static boolean COUNTER = true;

    // in "caution" mode, plenty of tests are done to protect ourselves
    // against errors in other components (ex: ReachingDefs)
    private static final boolean CAUTION = true;
    
    private CachingBBConverter bbconv;
    private ClassHierarchy ch;

    /** Creates a <code>MetaCallGraphImpl</code>. It must receive, in its
	last parameter, the <code>main</code> method of the program. */
    public MetaCallGraphImpl(CachingBBConverter bbconv, ClassHierarchy ch,
			     Set hmroots) {
        this.bbconv = bbconv;
	this.ch     = ch;

	// analyze all the roots
	for(Iterator it = hmroots.iterator(); it.hasNext(); )
	    analyze((HMethod) it.next());

	// convert the big format (Set oriented) into the compact one (arrays)
	compact();
	// activate the GC
	callees1    = null;
	callees2    = null;
	this.ch     = null;
	this.bbconv = null;
	analyzed_mm = null;
	WMM         = null;
	mm_work     = null;
	rdef        = null;
	ets2et      = null;
	mh2md       = null;
	param_types = null;
	// null these out so that we can serialize the object [CSA]
	// (alternatively, could mark all of these as 'transient')
	call_detector           = null;
	dd_wrapper              = null;
	dep_detection_visitor   = null;
	ti_wrapper              = null;
	type_inference_qvisitor = null;
	ets2et                  = null;
	ets_test                = null;
	// okay, now garbage-collect.
	System.gc();
    }

    // Converts the big format (Set oriented) into the compact one (arrays)
    // Takes the data from callees1(2) and puts it into callees1(2)_cmpct
    private void compact(){
	for(Iterator it = callees1.keys().iterator(); it.hasNext();){
	    MetaMethod mm = (MetaMethod) it.next();
	    Set callees = callees1.getValues(mm);
	    MetaMethod[] mms = 
		(MetaMethod[]) callees.toArray(new MetaMethod[callees.size()]);
	    callees1_cmpct.put(mm,mms);
	}

	for(Iterator it = callees2.keySet().iterator(); it.hasNext();){
	    MetaMethod mm = (MetaMethod) it.next();
	    Relation  rel = (Relation) callees2.get(mm);
	    Map map_cmpct = (Map) callees2_cmpct.get(mm);
	    if(map_cmpct == null)
		callees2_cmpct.put(mm,map_cmpct = new HashMap());
	    for(Iterator it_cs = rel.keys().iterator(); it_cs.hasNext();){
		CALL cs = (CALL) it_cs.next();
		// cees is the set of callees in "mm" at call site "cs".
		Set cees = rel.getValues(cs);
		MetaMethod[] mms =
		    (MetaMethod[]) cees.toArray(new MetaMethod[cees.size()]);
		map_cmpct.put(cs,mms);
	    }
	}
    }

    // Relation<MetaMethod, MetaMethod>  stores the association between
    //  the caller and its callees
    private Relation callees1 = new LightRelation();

    // Map<MetaMethod,Relation<CALL,MetaMethod>> stores the association
    // between the caller and its calees at a specific call site
    private Map      callees2 = new HashMap();    
    
    // Build up the meta call graph, starting from a "root" method.
    // The classic example of a root method is "main", but there could
    // be others called by the JVM before main.
    private void analyze(HMethod main_method){
	// we are extremely conservative: a root method could be called
	// with any subtypes for its arguments, so we treat it as a 
	// polymorphic one.
	analyze(new MetaMethod(main_method,true));

	if(DEBUG){
	    Set classes = ch.instantiatedClasses();
	    System.out.println(classes.size() + " instantiated class(es):");
	    for(Iterator it=classes.iterator();it.hasNext();)
		System.out.println(" " + (HClass)it.next());
	}
    }

    private PAWorkList WMM = null;
    private Set analyzed_mm = null;
    private MetaMethod mm_work = null;

    private int mm_count = 0;

    private void analyze(MetaMethod root_mm){
	if(COUNTER)
	    System.out.println("\n" + root_mm);

	analyzed_mm = new HashSet();
	WMM = new PAWorkList();
	WMM.add(root_mm);
	while(!WMM.isEmpty()){
	    mm_work = (MetaMethod) WMM.remove();

	    if(COUNTER){
		mm_count++;
		if(mm_count % 100 == 0)
		    System.out.println(mm_count + " analyzed meta-method(s)");
	    }

	    analyzed_mm.add(mm_work);
	    analyze_meta_method();
	}
	all_meta_methods.addAll(analyzed_mm);
	initial_set.clear();

	if(COUNTER)
	    System.out.println(mm_count + " analyzed meta-method(s)");
    }

    private final Set calls = new HashSet();
    private ReachingDefs rdef = null;

    private void analyze_meta_method(){

	if(DEBUG) System.out.println("\n\n%%: " + mm_work);

	HMethod hm = mm_work.getHMethod();

	// native method, no code for analysis  -> return immediately
	if(Modifier.isNative(hm.getModifiers())) return;

	BasicBlock.Factory bbf = bbconv.convert2bb(hm);
	HCode hcode = bbf.getHCode();
	rdef = new ReachingDefsImpl(hcode);

	// put the set of CALLs in the "calls".
	extract_the_calls(hcode);

	if(DEBUG){
	    System.out.println("Call sites: =======");
	    for(Iterator it = calls.iterator(); it.hasNext(); ){
		CALL q = (CALL) it.next();
		System.out.println(q.getLineNumber() + "\t" + q);
	    }
	    System.out.println("===================");
	}

	MethodData md = get_method_data(hm);
	SCComponent scc = md.last_scc;
	ets2et = md.ets2et;

	if(DEBUG){
	    System.out.println("SCC of ExactTemp definitions: =====");
	    for(SCComponent scc2=scc; scc2!=null; scc2=scc2.prevTopSort())
		System.out.println(scc2.toString());
	    System.out.println("===================================");
	}

	set_parameter_types(mm_work,hcode);
	compute_types(scc);

	analyze_calls();

	calls.clear();

	// before quiting this meta-method, remove the types we computed
	// for the ExactTemps. The component graph of ExactTemps (and so,
	// teh ExactTemps themselves) is specific to an HMethod and so, 
	// they can be shared by many meta-methods derived from the same
	// HMethod. Of course, I don't want the next analyzed meta-method
	// to use the types computed for this one.
	for(SCComponent scc2 = scc; scc2 != null; scc2 = scc2.prevTopSort())
	    for(Iterator it = scc2.nodes(); it.hasNext(); )
		((ExactTemp)it.next()).clearTypeSet();
    }


    // after the types of all the interesting ExactTemps are known,
    // examine each call site from this metamethod and build the
    // meta-call-graph.
    private void analyze_calls(){
	for(Iterator it_calls = calls.iterator();it_calls.hasNext();)
	    analyze_one_call((CALL) it_calls.next());
    }

    ////// "analyze_one_call" related stuff BEGIN ==========

    // Records the fact that mcaller could call mcallee at the call site cs.
    private void record_call(MetaMethod mcaller, CALL cs, MetaMethod mcallee){
	callees1.add(mcaller,mcallee);
	Relation rel = (Relation) callees2.get(mcaller);
	if(rel == null)
	    callees2.put(mcaller,rel = new LightRelation());
	rel.add(cs,mcallee);
    }

    // the specialized types will be stored here.
    private GenType[] param_types = null;

    // counts the number of metamethods called at a "virtual" call site.
    private int nb_meta_methods;

    // determine the exact meta-method which is called at the call site "cs",
    // based on the specialized types found in "param_types".
    private void specialize_the_call(HMethod hm, CALL cs){
	    if(DEBUG){
		System.out.println("hm  = " + hm);
		System.out.println("cs  = " + cs);
		System.out.print("param_types = [ ");
		for(int i = 0; i < param_types.length ; i++)
		    System.out.print(param_types[i] + " ");
		System.out.println("]");
	    }
	    MetaMethod mm_callee = new MetaMethod(hm,param_types);
	    nb_meta_methods++;
	    record_call(mm_work,cs,mm_callee);
	    if(!analyzed_mm.contains(mm_callee))
		WMM.add(mm_callee);
    }

    // "rec" generates all the possible combinations of types for the
    // parameters of "hm", generates metamethods, records the caller-callee
    // interactions and add the new metamethods to the worklist of metamethods.
    private void rec(HMethod hm, CALL cs, int pos, int max_pos){
	if(pos == max_pos){
	    // all the types have been specialized
	    specialize_the_call(hm,cs);
	    return;
	}

	if(cs.paramType(pos).isPrimitive()){
	    param_types[pos] = new GenType(cs.paramType(pos),GenType.MONO);
	    rec(hm,cs,pos+1,max_pos);
	    return;
	}

	// Go over all the possible reaching defs for the pos-th parameter
	// and over all the possible types for it. For each such type,
	// set param_types[pos] to it and recurse on the remaining params.
	Temp t = cs.params(pos);

	if(CAUTION && rdef.reachingDefs(cs,t).isEmpty())
	    Util.assert(false,"No reaching defs for " + t);

	Iterator it_rdef = rdef.reachingDefs(cs,t).iterator();
	while(it_rdef.hasNext()){
	    Quad qdef = (Quad) it_rdef.next(); 

	    if(CAUTION && getExactTemp(t,qdef).getTypeSet().isEmpty())
		Util.assert(false,"\nNo possible type detected for " + t +
			    "\n in method " + cs.getFactory().getMethod() +
			    "\n at instr  " + cs.getSourceFile() + ":" +
			    cs.getLineNumber() + " " + cs);

	    Iterator it_types = getExactTemp(t,qdef).getTypes();
	    while(it_types.hasNext()){
		GenType gt = (GenType) it_types.next();
		// fix some strange bug // TODO: study the impact of type "void"
		// if(gt.getHClass().isPrimitive()) return;
		param_types[pos] = gt;
		rec(hm,cs,pos+1,max_pos);
	    }
	}
    }

    // Checks whether calling "hm" starts a thread or not.
    private boolean thread_start_site(HMethod hm){
	String name = hm.getName();
	if(hm.getParameterNames().length != 0) return false;
	if((name==null) || !name.equals("start")) return false;
	if(hm.isStatic()) return false;
	HClass hclass = hm.getDeclaringClass();
	return hclass.getName().equals("java.lang.Thread");
    }

    private boolean found_a_run = false;

    // Examine a possible thread start site
    private boolean check_thread_start_site(CALL cs){
	if(!thread_start_site(cs.method())) return false;

	found_a_run = false;
	// tbase points to the receiver class
	Temp tbase = cs.params(0);

	// reaching defs for tbase
	Set rdefs = rdef.reachingDefs(cs, tbase);
	Util.assert(!rdefs.isEmpty(), 
		    "Thread start site with no reaching def: " + cs);
	
	for(Iterator it_qdef = rdefs.iterator(); it_qdef.hasNext(); ){
	    Quad qdef = (Quad) it_qdef.next();
	    // possible general types for tbase (as defined in line qdef)
	    Set pos_gts = getExactTemp(tbase, qdef).getTypeSet();
	    for(Iterator it_gt = pos_gts.iterator(); it_gt.hasNext(); ){
		GenType gt = (GenType) it_gt.next();
		
		if(gt.isPOLY()){
		    Set cls = get_instantiated_children(ch,gt.getHClass());
		    for(Iterator it_cls = cls.iterator(); it_cls.hasNext(); )
			check_thread_object(cs, (HClass) it_cls.next());
		}
		else
		    check_thread_object(cs, gt.getHClass());	
	    }
	}

	Util.assert(found_a_run,"No run method was found for " + cs);

	return true;
    }

    // hclass.start() might be called. Check it and detect the possible
    // "run" method.
    private void check_thread_object(CALL cs, HClass hclass){
	HMethod run = null;

	HMethod[] hms = hclass.getMethods();
	for(int i = 0; i < hms.length ; i++){
	    if(!hms[i].getName().equals("run")) continue;
	    int modifs = hms[i].getModifiers();
	    if(Modifier.isAbstract(modifs) ||
	       Modifier.isStatic(modifs) ||
	       Modifier.isNative(modifs) ||
	       !Modifier.isPublic(modifs)) continue;
	    if(hms[i].getParameterNames().length != 0) continue;
	    run = hms[i];
	    break;
	}
	
	if(run == null) return;

	// create a new MetaMethod corresponding to the run method and try
	// to put it into the worklist if it's really new.
	GenType gtbase = new GenType(hclass,GenType.MONO);
	MetaMethod mm  = new MetaMethod(run,new GenType[]{gtbase});
	if(!analyzed_mm.contains(mm))
	    WMM.add(mm);

	run_mms.add(mm);
	found_a_run = true;

	if(DEBUG)
	    System.out.println("THREAD START SITE:" + 
			       cs.getSourceFile() + ":" + 
			       cs.getLineNumber() + " " + 
			       cs + " => " + mm);
    }

    // analyze the CALL site "cs" inside the MetaMethod "mm".
    private void analyze_one_call(CALL cs){
	HMethod hm = cs.method();
	int nb_params = cs.paramsLength();
	param_types = new GenType[nb_params];

	if(DEBUG) System.out.println("$$:analyze_call(" + cs + ")");
	
	// for 'special' invocations, we know the method exactly
	if(!cs.isVirtual() || cs.isStatic()){
	    if(DEBUG) System.out.println("//: " + cs);
	    rec(hm,cs,0,nb_params);
	    return;
	}

	// for native methods, specialization doesn't make any sense
	// because we cannot analyze their body.
	if(Modifier.isNative(hm.getModifiers())){
	    check_thread_start_site(cs);
	    param_types[0] = new GenType(hm.getDeclaringClass(),GenType.POLY);
	    HClass[] types = hm.getParameterTypes();
	    for(int i = 0; i < types.length; i++)
		param_types[i+1] = new GenType(types[i],GenType.POLY);
	    specialize_the_call(hm, cs);
	    return;
	}

	nb_meta_methods = 0;

	if(nb_params == 0)
	    Util.assert(false,"Non static method with no parameters " + cs);

	// the first parameter (the method receiver) must be treated specially
	Temp tbase = cs.params(0);

	if(CAUTION && rdef.reachingDefs(cs,tbase).isEmpty())
	    Util.assert(false,"No reaching defs for " + tbase);

	Iterator it_rdef = rdef.reachingDefs(cs,tbase).iterator();
	while(it_rdef.hasNext()){
	    Quad qdef = (Quad) it_rdef.next();

	    if(CAUTION && getExactTemp(tbase,qdef).getTypeSet().isEmpty())
		Util.assert(false,"No possible type detected for <" + 
			    tbase + "," + qdef + ">");
	    
	    Iterator it_type_base = getExactTemp(tbase,qdef).getTypes();
	    while(it_type_base.hasNext()){
		GenType gt = (GenType) it_type_base.next();
		if(gt.isPOLY())
		    treat_poly_base(hm, cs, gt);
		else
		    treat_mono_base(hm, cs, gt);
	    }
	}

	if(DEBUG)
	    System.out.println("||: " + cs + " calls " + nb_meta_methods +
			       " meta-method(s)");

	if(DEBUG_CH && (nb_meta_methods == 0)){
	    System.out.println("ALARM!<\n" + "mm = " + mm_work + 
			       "\ncs = " + cs + "> 0 callees!");
	    it_rdef = rdef.reachingDefs(cs,tbase).iterator();
	    while(it_rdef.hasNext()){
		Quad qdef = (Quad) it_rdef.next();
		
		if(CAUTION && getExactTemp(tbase,qdef).getTypeSet().isEmpty())
		    Util.assert(false,"No possible type detected for <" + 
				tbase + "," + qdef + ">");
		
		Iterator it_type_base = getExactTemp(tbase,qdef).getTypes();
		while(it_type_base.hasNext()){
		    GenType gt = (GenType) it_type_base.next();
		    System.out.println(gt);
		}
	    }    
	}
	
	param_types = null; // enable the GC
    }

    // Treat the case of a polymorphic type for the receiver of the method hm
    private void treat_poly_base(HMethod hm, CALL cs, GenType gt){
	Set cls = get_possible_classes(gt.getHClass(),hm);
	for(Iterator it = cls.iterator(); it.hasNext();){
	    HClass c = (HClass) it.next();
	    // fix some strange bug
	    if(c.isPrimitive()) continue;
	    HMethod callee = c.getMethod(hm.getName(),hm.getDescriptor());
	    param_types[0] = new GenType(c,GenType.MONO);
	    rec(callee,cs,1,cs.paramsLength());
	}
    }

    // Returns the set of all the subclasses of root (including root) that
    // implement the method "hm".
    private Set get_possible_classes(HClass root, HMethod hm){
	Set children = MetaCallGraphImpl.get_instantiated_children(ch,root);
	Set possible_classes = new HashSet();

	for(Iterator it = children.iterator(); it.hasNext(); ){
	    HClass c = (HClass) it.next();
	    boolean implemented = true;
	    HMethod callee = null;
	    try{
		callee = c.getMethod(hm.getName(),hm.getDescriptor());
	    }catch(NoSuchMethodError nsme){
		implemented = false;
	    }
	    if(implemented && !Modifier.isAbstract(callee.getModifiers()))
		possible_classes.add(c);	    
	}
	
	return possible_classes;
    }
    
    // Returns the set of all the subclasses of class root that could
    // be instantiated by the program.
    static Set get_instantiated_children(ClassHierarchy ch, HClass root){
	Set children = new HashSet();
	PAWorkList Wc = new PAWorkList();
	Wc.add(root);
	while(!Wc.isEmpty()){
	    HClass c = (HClass) Wc.remove();
	    if(ch.instantiatedClasses().contains(c))
		children.add(c);
	    Iterator it_children = ch.children(c).iterator();
	    while(it_children.hasNext())
		Wc.add(it_children.next());
	}
	return children;
    }

    // Treat the case of a monomorphyc type for the receiver of the method hm
    private void treat_mono_base(HMethod hm, CALL cs, GenType gt){
	HClass c = gt.getHClass();
	// fix some strange bug: HClass.Void keeps appearing here!
	if(c.isPrimitive()) return;
	HMethod callee = null;
	boolean implements_hm = true;
	try{
	    callee = c.getMethod(hm.getName(),hm.getDescriptor());
	}
	catch(NoSuchMethodError nsme){
	    implements_hm = false;
	}
	if(implements_hm && !Modifier.isAbstract(callee.getModifiers())){
	    param_types[0] = gt;
	    rec(callee,cs,1,cs.paramsLength());
	}
    }

    ////// "analyze_one_call" related stuff END ===========

    // Quad visitor that scans the instructions and puts the CALLs in the set
    // "calls". Only one call_detector visitor is created for each instance of
    // MetaCallGraph - that's why it is created here, not
    // in extract_the_calls().
    private QuadVisitor call_detector = new QuadVisitor(){
	    public void visit(Quad q){
	    }
	    public void visit(CALL q){
		calls.add(q);
	    }
	};

    // Detects all the CALLs in hcode and puts them in the "calls" set.
    private void extract_the_calls(HCode hcode){
	Iterator it = hcode.getElementsI();
	while(it.hasNext()){
	    Quad q = (Quad) it.next();
	    q.accept(call_detector);
	}
    }


    // The set of the exact temps we are interested in (the arguments of
    // the CALLs from calls).
    private final Set initial_set = new HashSet();

    private final void build_initial_set(ReachingDefs rdef){
	initial_set.clear();
	Iterator it = calls.iterator();
	while(it.hasNext()){
	    CALL q = (CALL) it.next();
	    // System.out.println(" CALL SITE " + q);
	    int nb_params = q.paramsLength();
	    for(int i = 0; i < nb_params; i++)
		if(!q.paramType(i).isPrimitive()){
		    Temp t = q.params(i);
		    Iterator it2 = rdef.reachingDefs(q,t).iterator();
		    //System.out.println("UUUUUUU: " + t);
		    while(it2.hasNext()){
			Quad qdef = (Quad) it2.next();
			// System.out.println("  " + qdef);
			initial_set.add(getExactTemp(t,qdef));
		    }
		}
	}
    }

    // Data attached to a method
    private class MethodData{
	// The last scc (in decreasing topological order)
	SCComponent last_scc;
	// The ets2et map for that method (see the comments around ets2et
	Map ets2et;
	MethodData(SCComponent last_scc, Map ets2et){
	    this.last_scc = last_scc;
	    this.ets2et   = ets2et;
	}
    }

    // Map<HMethod,MethodData>
    private Map mh2md = new HashMap();
    // Adds some caching over "compute_md".
    private MethodData get_method_data(HMethod hm){
	MethodData md = (MethodData) mh2md.get(hm);
	if(md == null){
	    // initialize the ets2et map (see the comments near its definition)
	    ets2et = new HashMap();		    
	    build_initial_set(rdef);
	    SCComponent scc = compute_scc(initial_set,rdef);
	    mh2md.put(hm,md = new MethodData(scc,ets2et));
	}
	return md;
    }

    // an "exact" temp: the Temp "t" defined in the Quand "q"
    class ExactTemp{

	Quad q;
	Temp t;

	// Set<GenType> - the possible types of this ExactTemp.
	Set gtypes = new HashSet();

	// The ExactTemps whose types influence the type of this one.
	ExactTemp[] next = new ExactTemp[0];
	// The ExactTemps whose types are influence by the type of this one.
	ExactTemp[] prev = new ExactTemp[0];

	ExactTemp(Temp t, Quad q){
	    this.q = q;
	    this.t = t;
	}

	/** Returns an iterator over the set of all the possible types
	    for <code>this</code> <code>ExactTemp</code>. */
	Iterator getTypes(){
	    return gtypes.iterator();
	}

	/** Returns the set of all the possible types
	    for <code>this</code> <code>ExactTemp</code>. */
	Set getTypeSet(){
	    return gtypes;
	}

	/** Clears the set of possible types for <code>this</code> ExactTemp.*/
	void clearTypeSet(){
	    gtypes.clear();
	}

	/** Adds the type <code>gt</code> to the set of possible
	    types for <code>this</code> <code>ExactTemp</code>. */
	void addType(GenType type){

	    if(type.isPOLY()){
		// TODO : add some caching here
		Set children = 
	     MetaCallGraphImpl.get_instantiated_children(ch,type.getHClass());
		if(children.size() == 1)
		    type = new GenType((HClass)children.iterator().next(),
				       GenType.MONO);
	    }

	    Vector to_remove = new Vector();

	    for(Iterator it = gtypes.iterator() ; it.hasNext() ;){
		GenType gt = (GenType) it.next();
		if(type.included(gt,ch))
		    return;
		if(gt.included(type,ch))
		    to_remove.add(type);
	    }
	    
	    for(Enumeration enum=to_remove.elements();enum.hasMoreElements();)
		gtypes.remove((GenType) enum.nextElement());
	    
	    gtypes.add(type);
	}

	/** Adds the types from the set <code>gts</code> to the set of possible
	    types for <code>this</code> <code>ExactTemp</code>. */
	void addTypes(Set gts){
	    for(Iterator it = gts.iterator(); it.hasNext();)
		addType((GenType) it.next());
	}

	String shortDescription(){
	    return "<" + t.toString() + "\t, " + 
		q.getSourceFile() + ":" + q.getLineNumber() + "\t" +
		q.toString() + ">";
	}

	public String toString(){
	    StringBuffer buffer = new StringBuffer();
	    buffer.append(shortDescription());
	    if((next.length == 0) && (prev.length == 0)){
		buffer.append("\n");
		return buffer.toString();
	    }
	    buffer.append("(\n");
	    if(next.length > 0){
		buffer.append("Depends on the following defs:\n");
		for(int i = 0 ; i < next.length ; i++)
		    buffer.append("  " + 
				  (next[i]==null?"null":
				   next[i].shortDescription()) + "\n");
	    }
	    if(prev.length > 0){
		buffer.append("The following defs depends on this one:\n");
		for(int i = 0 ; i < prev.length ; i++)
		    buffer.append("  " + prev[i].shortDescription() + "\n");
	    }
	    buffer.append(")");
	    return buffer.toString();
	}
    }


    // Maps each ExactTemp to the set of its possible types
    // Relation<ExactTemp,GenType>
    // final Relation et2types = new Relation();


    ////// DEPENDENCY DETECTION (getDependencies) - BEGIN

    // Ugly stuff. Java inner classes cannot access anything but final data;
    // I want to create a single quad visitor instead of creating one each time
    // getDependencies is called; so, I cannot pass the parameters of
    // getDependencies to the constructor of the visitor. Instead, I have a 
    // final wrapper (that the quad visitor can access): each time
    // getDependencies is called, it puts some arguments into the wrapper's
    // fields, launch the visitor and finally retrieve the results from the 
    // wrapper.
    private class DDWrapper{
	ExactTemp[] deps = null;
	Temp           t = null;
    }
    private DDWrapper dd_wrapper = new DDWrapper();

    // Visitor for the dependency detection
    private QuadVisitor dep_detection_visitor = new QuadVisitor(){
	    public void visit(MOVE q){
		Temp t = dd_wrapper.t;
		if(CAUTION && !t.equals(q.dst())) stop_no_def(q);
		add_deps(q.src(),q);
	    }
	    
	    public void visit(AGET q){
		Temp t = dd_wrapper.t;
		if(CAUTION && !t.equals(q.dst())) stop_no_def(q);
		add_deps(q.objectref(),q);
	    }
	    
	    // The next "visit" functions don't do anything: the type 
	    // of "et" defined by the visited quad is fully determined by it.
	    public void visit(CALL q){
		if(CAUTION){
		    Temp t = dd_wrapper.t;
		    if(!t.equals(q.retval()) && !t.equals(q.retex()))
			stop_no_def(q);
		}
	    }

	    public void visit(NEW q){
		if(CAUTION){
		    Temp t = dd_wrapper.t;
		    if(!t.equals(q.dst())) stop_no_def(q);
		}
	    }
	    
	    public void visit(ANEW q){
		if(CAUTION){
		    Temp t = dd_wrapper.t;
		    if(!t.equals(q.dst())) stop_no_def(q);
		}
	    }

	    public void visit(TYPECAST q){
		if(CAUTION){
		    Temp t = dd_wrapper.t;
		    if(!t.equals(q.objectref())) stop_no_def(q);
		}

		//System.out.println("EEEEEEEEEEEEEEEEE");
	    }
	    
	    public void visit(GET q){
		if(CAUTION){
		    Temp t = dd_wrapper.t;
		    if(!t.equals(q.dst())) stop_no_def(q);
		}
	    }
	    
	    public void visit(METHOD q){
		// do nothing: the type of "et" defined here does not
		// depend on any other ExactType's type.
		if(CAUTION){
		    Temp t = dd_wrapper.t;
		    boolean found = false;
		    for(int i = 0; i < q.paramsLength(); i++)
			if(q.params(i).equals(t)){
			    found = true;
			    break;
			}
		    if(!found) stop_no_def(q);
		}
	    }
	    
	    public void visit(CONST q){
		if(CAUTION){
		    Temp t = dd_wrapper.t;
		    if(!t.equals(q.dst())) stop_no_def(q);
		}
		// do nothing; the type of "et" defined here does not
		// depend on any other ExactType's type.
	    }

	    // catch the forgotten quads
	    public void visit(Quad q){
		Util.assert(false,"Unsupported Quad " + q);		    
	    }
	    
	    // crash the system in the most spectacular way
	    private void stop_no_def(Quad q){
		Temp t = dd_wrapper.t;
		Util.assert(false,q + " doesn't define " + t);
	    }
	    
	    // The Temp "tdep" is used in quad "q" to define "t" and
	    // can affect its type. This function qoes along the list
	    // of possible definitions for tdep and put the corresponding
	    // ExactTemp's in the array of dependencies for <t,q>.
	    private void add_deps(Temp tdep, Quad q){
		Temp t = dd_wrapper.t;
		Set reaching_defs = rdef.reachingDefs(q,tdep);
		if(reaching_defs.isEmpty())
		    Util.assert(false,"Temp " + t + " in " + q +
				    " has no reaching definition!");
		dd_wrapper.deps = new ExactTemp[reaching_defs.size()];
		int i = 0;
		Iterator it_defs = reaching_defs.iterator();
		while(it_defs.hasNext()){
		    Quad qdef = (Quad) it_defs.next();
		    dd_wrapper.deps[i] = getExactTemp(tdep,qdef);
		    i++;
		}
	    }
	};
    

    // Returns all the ExactTemps whose types influence the type of the
    // ExactType et.
    private ExactTemp[] getDependencies(final ExactTemp et){
	dd_wrapper.t    = et.t;
	dd_wrapper.deps = null;
	et.q.accept(dep_detection_visitor);
	if(dd_wrapper.deps == null)
	    return (new ExactTemp[0]);
	return dd_wrapper.deps;
    }

    ////// DEPENDENCY DETECTION (getDependencies) - END

    // The following code is responsible for assuring that at most one
    // ExactTemp object representing the temp t defined at quad q exists
    // at the execution time. This is necessary since we keep the next and
    // prev info *into* the ExactTemp structure and not in a map (for
    // efficiency reasons), so all the info related to a conceptual ExactTemp
    // must be in a single place.
    // This is enforced by having a Map<ExactTempI,ExactTemp> that maps
    // an ExactTempS (just a <t,q> couple) to the full ExactTemp object.
    // When we need the ExactTemp for a given <t,q>
    // pair, we search <t,q> in that map; if found we return the existent 
    // object, otherwise we create a new one and put it into the map.
    // Of course, no part of the program should dirrectly allocate an
    // ExactTemp, instead the "getExactTemp" function should be called. 
    private class ExactTempS{ // S for short
	Temp t;
	Quad q;
	ExactTempS(Temp t, Quad q){
	    this.t = t;
	    this.q = q;
	}
	public int hashCode(){
	    return t.hashCode() + q.hashCode();
	}
	public boolean equals(Object o){
	    ExactTempS ets2 = (ExactTempS) o;
	    return (this.t == ets2.t) && (this.q == ets2.q);
	}
    }
    private Map ets2et = null;
    private ExactTempS ets_test = new ExactTempS(null,null);
    private ExactTemp getExactTemp(Temp t, Quad q){
	ets_test.t = t;
	ets_test.q = q;
	ExactTemp et = (ExactTemp) ets2et.get(ets_test);
	if(et == null){
	    et = new ExactTemp(t,q);
	    ExactTempS ets = new ExactTempS(t,q);
	    ets2et.put(ets,et);
	}
	return et;
    }
    // the end of "unique ExactTemp" code.
    

    // Computes the topologically sorted list of strongly connected
    // components containing the definitions of the "interesting" temps.
    // The edges between this nodes models the relation
    // "definition x uses the exact temp introduced by the definition y".
    // Returns a structure consisting of the last element of the sorted list
    // (in decreasing topological order)
    // (the rest can be retrieved by navigating with prevTopSort()) and the
    // ets2et map that assures the unicity of ExactTemps associated with the
    // same <t,q> pair.
    private SCComponent compute_scc(Set initial_set, final ReachingDefs rdef){
	// navigator into the graph of ExactTemps: next returns the ExactTemps
	// whose types influence the type of "node" ExactTemp; prev returns
	// teh ExactTemps whose types is affected by the type of node.  
	SCComponent.Navigator et_navigator =
	    new SCComponent.Navigator(){
		    public Object[] next(Object node){
			return ((ExactTemp)node).next;
		    }
		    public Object[] prev(Object node){
			return ((ExactTemp)node).prev;
		    }
		};

	// first, compute the successors and the predecessors of each node
	// (ExactTemp); the successors are put into the next field of the
	// the ExactTemps; predecessors are not put into the ExactTemps, but
	// into a separate relation - "prev_rel".
	Relation prev_rel = new LightRelation();
	Set already_visited = new HashSet();
	PAWorkList W = new PAWorkList();
	W.addAll(initial_set);
	while(!W.isEmpty()){
	    ExactTemp et = (ExactTemp) W.remove();
	    already_visited.add(et);

	    et.next = getDependencies(et);

	    //System.out.println("RRRRRRRRR: " + et + " <- { ");
	    //for(int i = 0; i < et.next.length; i++)
	    //System.out.print(et.next[i] + " ");
	    //System.out.println("}");

	    for(int i = 0; i < et.next.length; i++){
		ExactTemp et2 = (ExactTemp) et.next[i];

		if(et2 == null)
		    Util.assert(false,"Something wrong with " + et);

		prev_rel.add(et2,et);
		if(!already_visited.contains(et2))
		    W.add(et2);
	    }
	}

	// now, put the predecessors in the ExactTemps
	for(Iterator it = prev_rel.keys().iterator(); it.hasNext(); ) {
	    ExactTemp et = (ExactTemp) it.next();
	    Set values = prev_rel.getValues(et);
	    et.prev = 
		(ExactTemp[]) values.toArray(new ExactTemp[values.size()]);
	}

	// build the component graph and sort it topollogically
	Set scc_set = SCComponent.buildSCC(initial_set, et_navigator);
	SCCTopSortedGraph ts_scc = SCCTopSortedGraph.topSort(scc_set);

	return ts_scc.getLast();
    }


    // Set the types of the parameters of the method underlying mm, using the
    // appropriate type specializations.
    private void set_parameter_types(MetaMethod mm, HCode hcode){
	METHOD m = (METHOD) ((HEADER) hcode.getRootElement()).next(1);

	int nb_params = m.paramsLength();
	if(CAUTION && (nb_params != mm.nbParams()))
	    Util.assert(false,"Wrong number of params in " + m);

	for(int i = 0; i < nb_params ; i++)
	    getExactTemp(m.params(i),m).addType(mm.getType(i));
    }


    // computes the types of the interesting ExactTemps, starting 
    // with those in the strongly connected component "scc".
    private void compute_types(SCComponent scc){
	SCComponent p = scc;
	while(p != null){
	    process_scc(p);
	    p = p.prevTopSort();
	}
    }


    // TYPE INFERENCE QUAD VISITOR - BEGIN

    // For more or less the same reasons as for the dependecy detection, I use
    // a final wrapper containing the arguments and the results of the visitor.
    private class TIWrapper{
	Temp       t = null;
	ExactTemp et = null;
    };
    private TIWrapper ti_wrapper = new TIWrapper();

    private QuadVisitor type_inference_qvisitor = new QuadVisitor(){
	    
	    public void visit(MOVE q){
		Temp       t = ti_wrapper.t;
		ExactTemp et = ti_wrapper.et;
		if(CAUTION && !t.equals(q.dst()))
		    stop_no_def(q);
		for(int i = 0; i < et.next.length ; i++)
		    et.addTypes(et.next[i].getTypeSet());
	    }
	    
	    public void visit(GET q){
		Temp       t = ti_wrapper.t;
		ExactTemp et = ti_wrapper.et;
		if(CAUTION && !t.equals(q.dst()))
		    stop_no_def(q);
		et.addType(new GenType(q.field().getType(),GenType.POLY));
	    }
	    
	    public void visit(AGET q){
		Temp       t = ti_wrapper.t;
		ExactTemp et = ti_wrapper.et;
		if(CAUTION && !t.equals(q.dst()))
		    stop_no_def(q);
		
		// For all the possible types for the source array, take
		// the type of the component 
		
		// The Temp representing the array
		Temp ta = q.objectref();
		Set reaching_defs = rdef.reachingDefs(q,ta);
		if(reaching_defs.isEmpty())
		    Util.assert(false,"No reaching defs for "+ta+" in "+q);
		
		Iterator it_q = reaching_defs.iterator();
		while(it_q.hasNext()){
		    Quad qdef = (Quad) it_q.next();
		    ExactTemp eta = getExactTemp(ta,qdef);
		    Iterator it_eta_types = eta.getTypes();
		    while(it_eta_types.hasNext()){
			HClass c = ((GenType) it_eta_types.next()).getHClass();
			if(c.equals(HClass.Void))
			    et.addType(new GenType(HClass.Void, GenType.MONO));
			else{
			    HClass hcomp = c.getComponentType();
			    if(hcomp == null)
				Util.assert(false,ta + " could have non-array"+
					    " types in " + q);
			    et.addType(new GenType(hcomp,GenType.POLY));
			}
		    }
		}
	    }
	    
	    // Aux. data for visit(CALL q).
	    // Any method can throw an exception that is subclass of these
	    // two classes (without explicitly declaring it).
	    private static final HClass jl_RuntimeException =
		Loader.systemLinker.forName("java.lang.RuntimeException");
	    private static final HClass jl_Error =
		Loader.systemLinker.forName("java.lang.Error");

	    public void visit(CALL q){
		Temp       t = ti_wrapper.t;
		ExactTemp et = ti_wrapper.et;
		if(t.equals(q.retval())){
			et.addType(new GenType(q.method().getReturnType(),
					       GenType.POLY));
			return;
		}
		if(t.equals(q.retex())){
		    HClass[] excp = q.method().getExceptionTypes();
		    for(int i=0; i<excp.length; i++)
			et.addType(new GenType(excp[i],GenType.POLY));
		    // According to the JLS, exceptions that are subclasses of
		    // java.lang.RuntimeException and java.lang.Error need
		    // not be explicitly declared; they can be thrown by any
		    // method.
		    et.addType(new GenType(jl_RuntimeException, GenType.POLY));
		    et.addType(new GenType(jl_Error, GenType.POLY));
		    return;
		}
		stop_no_def(q);
	    }
	    
	    public void visit(NEW q){
		Temp       t = ti_wrapper.t;
		ExactTemp et = ti_wrapper.et;
		if(CAUTION && !t.equals(q.dst()))
		    stop_no_def(q);
		et.addType(new GenType(q.hclass(),GenType.MONO));
	    }
	    
	    public void visit(ANEW q){
		Temp       t = ti_wrapper.t;
		ExactTemp et = ti_wrapper.et;
		if(CAUTION && !t.equals(q.dst()))
		    stop_no_def(q);
		et.addType(new GenType(q.hclass(),GenType.MONO));
	    }
	    
	    public void visit(TYPECAST q){
		Temp       t = ti_wrapper.t;
		ExactTemp et = ti_wrapper.et;
		
		//System.out.println("DDDDDDDDDDDDDDDD");

		if(CAUTION && !t.equals(q.objectref()))
		    stop_no_def(q);
		et.addType(new GenType(q.hclass(), GenType.POLY));
	    }
	    
	    public void visit(METHOD q){
		// do nothing; the types of the parameters have been
		// already set by set_parameter_types.
		if(CAUTION){
		    Temp t = ti_wrapper.t;
		    boolean found = false;
		    for(int i = 0; i < q.paramsLength(); i++)
			if(q.params(i).equals(t)){
			    found = true;
			    break;
			}
		    if(!found) stop_no_def(q);
		}
	    }
	    
	    public void visit(CONST q){
		if(CAUTION){
		    Temp t = ti_wrapper.t;
		    if(!t.equals(q.dst())) stop_no_def(q);
		}
		ExactTemp et = ti_wrapper.et;
		et.addType(new GenType(q.type(), GenType.MONO));
	    }

	    public void visit(Quad q){
		Util.assert(false,"Unsupported Quad " + q);
	    }
	    
	    private void stop_no_def(Quad q){
		Util.assert(false,q + " doesn't define " + ti_wrapper.t);
	    }
	    
	};

    // TYPE INFERENCE QUAD VISITOR - END
    
    
    private void process_scc(SCComponent scc){
	final PAWorkList W = new PAWorkList();

	if(DEBUG)
	    System.out.println("Processing " + scc);
	
	W.addAll(scc.nodeSet());
	while(!W.isEmpty()){
	    ExactTemp et = (ExactTemp) W.remove();

	    ti_wrapper.t  = et.t;
	    ti_wrapper.et = et;

	    Set old_gen_type = new HashSet(et.getTypeSet()); 
	    et.q.accept(type_inference_qvisitor);
	    Set new_gen_type = et.getTypeSet();

	    if(!new_gen_type.equals(old_gen_type))
		for(int i = 0; i < et.prev.length; i++){
		    ExactTemp et2 = et.prev[i];
		    if(scc.contains(et2)) W.add(et);
		}
	}

	if(DEBUG)
	    for(Iterator it = scc.nodes(); it.hasNext();){
		ExactTemp et = (ExactTemp) it.next();
		System.out.println("##:< " + et.shortDescription() + 
				   " -> " + et.getTypeSet() + " >");
	    }
    }

}

