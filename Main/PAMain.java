// PAMain.java, created Fri Jan 14 10:54:16 2000 by salcianu
// Copyright (C) 1999 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Main;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HMethod;
import harpoon.ClassFile.HCodeFactory;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.CachingCodeFactory;
import harpoon.ClassFile.Linker;
import harpoon.ClassFile.Loader;

import harpoon.IR.Quads.Quad;
import harpoon.IR.Quads.QuadVisitor;
import harpoon.IR.Quads.QuadWithTry;

import harpoon.Analysis.Quads.QuadClassHierarchy;
import harpoon.Analysis.Quads.CallGraph;
import harpoon.Analysis.Quads.CallGraphImpl;
import harpoon.Analysis.AllCallers;
import harpoon.Analysis.ClassHierarchy;
import harpoon.Analysis.BasicBlock;

import harpoon.Analysis.PointerAnalysis.PointerAnalysis;
import harpoon.Analysis.PointerAnalysis.PANode;
import harpoon.Analysis.PointerAnalysis.ParIntGraph;
import harpoon.Util.DataStructs.Relation;
import harpoon.Analysis.PointerAnalysis.MAInfo;
import harpoon.Analysis.PointerAnalysis.SyncElimination;
import harpoon.Analysis.PointerAnalysis.InstrumentSyncOps;

import harpoon.Analysis.MetaMethods.MetaMethod;
import harpoon.Analysis.MetaMethods.MetaCallGraph;
import harpoon.Analysis.MetaMethods.FakeMetaCallGraph;
import harpoon.Analysis.MetaMethods.MetaCallGraphImpl;
import harpoon.Analysis.MetaMethods.MetaAllCallers;

import harpoon.Util.BasicBlocks.BBConverter;
import harpoon.Util.BasicBlocks.CachingBBConverter;
import harpoon.Util.LightBasicBlocks.LightBasicBlock;
import harpoon.Util.LightBasicBlocks.LBBConverter;
import harpoon.Util.LightBasicBlocks.CachingLBBConverter;
import harpoon.Util.Graphs.SCComponent;
import harpoon.Util.Graphs.SCCTopSortedGraph;
import harpoon.Util.WorkSet;

import harpoon.Analysis.PointerAnalysis.Debug;
import harpoon.Analysis.MetaMethods.SmartCallGraph;

import harpoon.IR.Quads.CALL;

import harpoon.IR.Jasmin.Jasmin;


/**
 * <code>PAMain</code> is a simple Pointer Analysis top-level class.
 * It is designed for testing and evaluation only.
 * 
 * @author  Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
 * @version $Id: PAMain.java,v 1.1.2.74 2000-07-15 15:07:05 jwhaley Exp $
 */
public abstract class PAMain {

    // use the real meta call graph
    private static boolean METAMETHODS = false;
    // use FakeMetaCallGraph(SmartCallGraph)
    private static boolean SMART_CALL_GRAPH = false;
    // debug the class hierarchy
    private static boolean SHOW_CH = false;

    // show the call graph
    private static boolean SHOW_CG = false;
    // show the split relation HMethod -> MetaMethods
    private static boolean SHOW_SPLIT = false;
    // show some details/statistics about the analysis
    private static boolean SHOW_DETAILS = false;

    // make the program to analyse some method;
    private static boolean DO_ANALYSIS = false;
    // turns on the interactive analysis
    private static boolean DO_INTERACTIVE_ANALYSIS = false;

    // turns on the computation of the memory allocation policies
    private static boolean MA_MAPS = false;
    // the name of the file into which the memory allocation policies
    // will be serialized
    private static String MA_MAPS_OUTPUT_FILE = null;
    // use the inter-thread stage of the analysis while determining the
    // memory allocation policies
    private static boolean USE_INTER_THREAD = true;

    // Displays some help messages.
    private static boolean DEBUG = false;

    private static boolean DO_SAT = false;
    private static String SAT_FILE = null;

    private static boolean ELIM_SYNCOPS = false;
    private static boolean INST_SYNCOPS = false;

    private static boolean DUMP_JAVA = false;
    
    private static boolean COMPILE = false;
    
    // Load the preanalysis results from PRE_ANALYSIS_IN_FILE
    private static boolean LOAD_PRE_ANALYSIS = false;
    private static String PRE_ANALYSIS_IN_FILE = null;

    // Save the preanalysis results into PRE_ANALYSIS_OUT_FILE
    private static boolean SAVE_PRE_ANALYSIS = false;
    private static String PRE_ANALYSIS_OUT_FILE = null;

    private static PointerAnalysis pa = null;
    // the main method
    private static HMethod hroot = null;

    // list to maintain the methods to be analyzed
    private static List mm_to_analyze = new LinkedList();
    
    private static class Method implements java.io.Serializable {
	String name  = null;
	String declClass = null;
	public boolean equals(Object o) {
	    if((o == null) || !(o instanceof Method))
		return false;
	    Method m2 = (Method) o;
	    return 
		str_equals(name, m2.name) &&
		str_equals(declClass, m2.declClass);
	}
	public static boolean str_equals(String s1, String s2) {
	    return (s1 == null) ? (s2 == null) : s1.equals(s2); 
	}
	public String toString() {
	    return declClass + "." + name;
	}
    }

    private static Method root_method = new Method();


    private static Linker linker = Loader.systemLinker;
    private static CachingCodeFactory hcf = null;
    private static MetaCallGraph  mcg = null;
    private static MetaAllCallers mac = null;
    private static Relation split_rel = null;
    private static CachingBBConverter bbconv = null;
    private static LBBConverter lbbconv = null;
    // the class hierarchy of the analyzed program
    private static ClassHierarchy ch = null;


    // The set of method roots, i.e. those methods that represents
    // entry points into the call graph:
    //  1. the methods that might be called by the runtime system
    //  2. the static initializers of all the instantiated classes and
    //  3. the "main" method of the analyzed program.
    private static Set mroots = null;

    // The set of classes and methods that are instantiated/called by
    // the current implementation of the runtime.
    private static Set runtime_callable = new HashSet
	(harpoon.Backend.Runtime1.Runtime.runtimeCallableMethods(linker));

    // global variable used for timing measurements
    private static long g_tstart = 0L;

    public static void main(String[] params) throws IOException {
	int optind = get_options(params);
	int nbargs = params.length - optind;
	if(nbargs < 1){
	    show_help();
	    System.exit(1);
	}
	print_options();

	get_root_method(params[optind]);
	if(hroot == null){
	    System.out.println("Sorry, the root method was not found\n");
	    System.exit(1);
	}
	System.out.println("Root method: " + root_method.declClass + "." +
			   root_method.name);

	if(LOAD_PRE_ANALYSIS)
	    load_pre_analysis();
	else {
	    pre_analysis();
	    if(SAVE_PRE_ANALYSIS)
		save_pre_analysis();
	}

	/* JOIN STATS
	   join_stats(lbbconv, mcg);
	   System.exit(1);
	*/

	pa = new PointerAnalysis(mcg, mac, lbbconv);

	if(DO_ANALYSIS)
	    do_analysis();

	if(DO_INTERACTIVE_ANALYSIS)
	    do_interactive_analysis();
    
	if(MA_MAPS)
	    ma_maps();

	if(DO_SAT)
	    do_sat();

	if(SHOW_DETAILS)
	    pa.print_stats();

        if (ELIM_SYNCOPS)
            do_elim_syncops();

	if(DUMP_JAVA)
	    dump_java(get_classes(pa.getMetaCallGraph().getAllMetaMethods()));

	if(COMPILE) {
	    SAMain.hcf = hcf;
	    SAMain.className = params[optind];
	    SAMain.do_it();
	}
    }
    
    // Constructs some data structures used by the analysis: the code factory
    // providing the code of the methods, the class hierarchy, call graph etc.
    private static void pre_analysis() {
	g_tstart = System.currentTimeMillis();
	
	hcf = new CachingCodeFactory(harpoon.IR.Quads.QuadNoSSA.codeFactory(),
				     true);
	bbconv = new CachingBBConverter(hcf);
	lbbconv = new CachingLBBConverter(bbconv);
	
	construct_class_hierarchy();
	construct_mroots();
	construct_meta_call_graph();
	construct_split_relation();
	
	System.out.println("Total pre-analysis time : " +
			   (time() - g_tstart) + "ms");
    }

    // load the results of the preanalysis from the disk
    private static void load_pre_analysis() {
	try{
	    System.out.print("Loading preanalysis results from " + 
			     PRE_ANALYSIS_IN_FILE + " ... ");
	    g_tstart = time();
	    load_pre_analysis2();
	    System.out.println((time() - g_tstart) + "ms");
	} catch(Exception e){
	    System.err.println("\nError while loading pre-analysis results!");
	    System.err.println(e);
	    System.exit(1);
	}
    }
    
    // do the real job behind load_pre_analysis
    private static void load_pre_analysis2()
	throws IOException, ClassNotFoundException {
	ObjectInputStream ois = new ObjectInputStream
	    (new FileInputStream(PRE_ANALYSIS_IN_FILE));
	Method m2 = (Method) ois.readObject();
	if((m2 == null) || !m2.equals(root_method)) {
	    System.err.println("\nDifferent root method: " + m2 + "!");
	    System.exit(1);
	}
	linker    = (Linker) ois.readObject();
	hcf       = (CachingCodeFactory) ois.readObject();
	bbconv    = (CachingBBConverter) ois.readObject();
	lbbconv   = (LBBConverter) ois.readObject();
	ch        = (ClassHierarchy) ois.readObject();
	mroots    = (Set) ois.readObject();
	mcg       = (MetaCallGraph) ois.readObject();
	mac       = (MetaAllCallers) ois.readObject();
	split_rel = (Relation) ois.readObject();
	ois.close();
    }


    // save the results of the preanalysis for future use
    private static void save_pre_analysis() {
	try{
	    System.out.print("Saving preanalysis results into " + 
			     PRE_ANALYSIS_OUT_FILE + " ... ");
	    g_tstart = time();
	    save_pre_analysis2();
	    System.out.println((time() - g_tstart) + "ms");
	} catch(IOException e){
	    System.err.println("\nError while saving pre-analysis results!");
	    System.err.println(e);
	    System.exit(1);
	}
    }

    // do the real job  behind save_pre_analysis
    private static void save_pre_analysis2() throws IOException {
	ObjectOutputStream oos = new ObjectOutputStream
	    (new FileOutputStream(PRE_ANALYSIS_OUT_FILE));
	oos.writeObject(root_method);
	oos.writeObject(linker);
	oos.writeObject(hcf);
	oos.writeObject(bbconv);
	oos.writeObject(lbbconv);
	oos.writeObject(ch);
	oos.writeObject(mroots);
	oos.writeObject(mcg);
	oos.writeObject(mac);
	oos.writeObject(split_rel);
	oos.flush();
	oos.close();
    }


    // Finds the root method: the "main" method of "class".
    private static void get_root_method(String root_class) {
	root_method.name = "main";
	root_method.declClass = root_class;
	HClass hclass = linker.forName(root_method.declClass);
	HMethod[] hm  = hclass.getDeclaredMethods();

	// search for the main method
	hroot = null;
	for(int i = 0;i<hm.length;i++)
	    if(hm[i].getName().equals(root_method.name))
		hroot = hm[i];
    }


    private static void display_method(Method method)
	throws harpoon.ClassFile.NoSuchClassException {

	HClass hclass = linker.forName(method.declClass);
	HMethod[] hm  = hclass.getDeclaredMethods();
	int nbmm = 0;

	HMethod hmethod = null;		
	for(int i = 0; i < hm.length; i++)
	    if(hm[i].getName().equals(method.name)){
		hmethod = hm[i];

		// look for all the meta-methods originating into this method
		// and do all the analysis stuff on them.
		for(Iterator it = split_rel.getValues(hmethod).iterator();
		    it.hasNext();){
		    nbmm++;
		    MetaMethod mm = (MetaMethod) it.next();
		    System.out.println("HMETHOD " + hmethod
				       + " ->\n META-METHOD " + mm);
		    ParIntGraph int_pig = pa.getIntParIntGraph(mm);
		    ParIntGraph ext_pig = pa.getExtParIntGraph(mm);
		    ParIntGraph pig_inter_thread = pa.threadInteraction(mm);
		    PANode[] nodes = pa.getParamNodes(mm);
		    System.out.println("META-METHOD " + mm);
		    System.out.print("POINTER PARAMETERS: ");
		    System.out.print("[ ");
		    for(int j = 0; j < nodes.length; j++)
			System.out.print(nodes[j] + " ");
		    System.out.println("]");
		    System.out.print("INT. GRAPH AT THE END OF THE METHOD:");
		    System.out.println(int_pig);
		    System.out.print("EXT. GRAPH AT THE END OF THE METHOD:");
		    System.out.println(ext_pig);
		    System.out.print("INT. GRAPH AT THE END OF THE METHOD" +
				     " + INTER-THREAD ANALYSIS:");
		    System.out.println(pig_inter_thread);
		    
		}
	    }

	if (INST_SYNCOPS)
	    do_inst_syncops(hmethod);
	
	if (ELIM_SYNCOPS)
	    do_elim_syncops(hmethod);
    
	if(hmethod == null){
	    System.out.println("Oops!" + method.declClass + "." +
			       method.name + " not found");
	    return;
	}

	if(nbmm == 0)
	    System.out.println("Oops!" + method.declClass + "." +
			       method.name +
			       " seems not to be called at all");
	else
	    System.out.println(nbmm + " ANALYZED META-METHOD(S)");

    }

    // receives a "class.name" string and cut it into pieces, separating
    // the name of the class from the name of the method.
    private static Method getMethodName(String str){
	Method method = new Method();
	int point_pos = str.lastIndexOf('.');
	method.name           = str.substring(point_pos+1);
	if(point_pos == -1) return method;
	method.declClass      = str.substring(0,point_pos);
	return method;
    }

    // process the command line options; returns the starting index of
    // the non-option arguments
    private static int get_options(String[] argv){
	int c, c2;
	String arg;
	LongOpt[] longopts = new LongOpt[]{
	    new LongOpt("meta",      LongOpt.NO_ARGUMENT,       null, 'm'),
	    new LongOpt("smartcg",   LongOpt.NO_ARGUMENT,       null, 's'),
	    new LongOpt("showch",    LongOpt.NO_ARGUMENT,       null, 'c'),
	    new LongOpt("ccs",       LongOpt.REQUIRED_ARGUMENT, null, 5),
	    new LongOpt("ts",        LongOpt.NO_ARGUMENT,       null, 6),
	    new LongOpt("wts",       LongOpt.NO_ARGUMENT,       null, 7),
	    new LongOpt("ls",        LongOpt.NO_ARGUMENT,       null, 8),
	    new LongOpt("showcg",    LongOpt.NO_ARGUMENT,       null, 9),
	    new LongOpt("showsplit", LongOpt.NO_ARGUMENT,       null, 10),
	    new LongOpt("details",   LongOpt.NO_ARGUMENT,       null, 11),
	    new LongOpt("mamaps",    LongOpt.REQUIRED_ARGUMENT, null, 14),
	    new LongOpt("noit",      LongOpt.NO_ARGUMENT,       null, 15),
	    new LongOpt("inline",    LongOpt.NO_ARGUMENT,       null, 16),
	    new LongOpt("sat",       LongOpt.REQUIRED_ARGUMENT, null, 17),
	    new LongOpt("notg",      LongOpt.NO_ARGUMENT,       null, 18),
	    new LongOpt("loadpre",   LongOpt.REQUIRED_ARGUMENT, null, 19),
	    new LongOpt("savepre",   LongOpt.REQUIRED_ARGUMENT, null, 20),
	    new LongOpt("syncelim",  LongOpt.NO_ARGUMENT,       null, 21),
	    new LongOpt("instsync",  LongOpt.NO_ARGUMENT,       null, 22),
	    new LongOpt("dumpjava",  LongOpt.NO_ARGUMENT,       null, 23),
	    new LongOpt("backend",   LongOpt.REQUIRED_ARGUMENT, null, 'b'),
	    new LongOpt("output",    LongOpt.REQUIRED_ARGUMENT, null, 'o'),
	};

	Getopt g = new Getopt("PAMain", argv, "mscoa:i", longopts);

	while((c = g.getopt()) != -1)
	    switch(c){
	    case 'm':
		SMART_CALL_GRAPH = false;
		METAMETHODS = true;
		break;
	    case 's':
		METAMETHODS = false;
		SMART_CALL_GRAPH = true;
		break;
	    case 'c':
		SHOW_CH = true;
		break;
	    case 'a':
		DO_ANALYSIS = true;
		mm_to_analyze.add(getMethodName(g.getOptarg()));
		break;
	    case 'i':
		DO_INTERACTIVE_ANALYSIS = true;
		break;
	    case 5:
		arg = g.getOptarg();
		PointerAnalysis.CALL_CONTEXT_SENSITIVE = true;
		PointerAnalysis.MAX_SPEC_DEPTH = new Integer(arg).intValue();
		break;
	    case 6:
		PointerAnalysis.THREAD_SENSITIVE = true;
		PointerAnalysis.WEAKLY_THREAD_SENSITIVE = false;
		break;
	    case 7:
		PointerAnalysis.WEAKLY_THREAD_SENSITIVE = true;
		PointerAnalysis.THREAD_SENSITIVE = false;
		break;
	    case 8:
		PointerAnalysis.LOOP_SENSITIVE = true;
		break;
	    case 9:
		SHOW_CG = true;
		break;
	    case 10:
		SHOW_SPLIT = true;
		break;
	    case 11:
		SHOW_DETAILS = true;
		break;
	    case 14:
		MA_MAPS = true;
		MA_MAPS_OUTPUT_FILE = new String(g.getOptarg());
		break;
	    case 15:
		USE_INTER_THREAD = false;
		break;
	    case 16:
		MAInfo.DO_METHOD_INLINING = true;
		break;
	    case 17:
		DO_SAT = true;
		SAT_FILE = new String(g.getOptarg());
		break;
	    case 18:
		MAInfo.NO_TG = true;
		break;
	    case 19:
		LOAD_PRE_ANALYSIS = true;
		PRE_ANALYSIS_IN_FILE = new String(g.getOptarg());
		break;
	    case 20:
		SAVE_PRE_ANALYSIS = true;
		PRE_ANALYSIS_OUT_FILE = new String(g.getOptarg());
		break;		
	    case 21:
		ELIM_SYNCOPS = true;
		break;
	    case 22:
		INST_SYNCOPS = true;
		break;
	    case 23:
		DUMP_JAVA = true;
		break;
	    case 'o':
		SAMain.ASSEM_DIR = new java.io.File(g.getOptarg());
		harpoon.Util.Util.assert(SAMain.ASSEM_DIR.isDirectory(), ""+SAMain.ASSEM_DIR+" must be a directory");
		break;
	    case 'b': {
		COMPILE = true;
		String backendName = g.getOptarg().toLowerCase().intern();
		if (backendName == "strongarm")
		    SAMain.BACKEND = SAMain.STRONGARM_BACKEND;
		if (backendName == "sparc")
		    SAMain.BACKEND = SAMain.SPARC_BACKEND;
		if (backendName == "mips")
		    SAMain.BACKEND = SAMain.MIPS_BACKEND;
		if (backendName == "precisec")
		    SAMain.BACKEND = SAMain.PRECISEC_BACKEND;
		break;
	    }
	    }

	return g.getOptind();
    }

    private static void print_options(){
	if(METAMETHODS && SMART_CALL_GRAPH){
	    System.out.println("Call Graph Type Ambiguity");
	    System.exit(1);
	}
	System.out.print("Execution options:");

	if(LOAD_PRE_ANALYSIS)
	    System.out.print("LOAD_PRE_ANALYSIS ("+PRE_ANALYSIS_IN_FILE+")");
	if(SAVE_PRE_ANALYSIS)
	    System.out.print("SAVE_PRE_ANALYSIS ("+PRE_ANALYSIS_OUT_FILE+")");
	if(METAMETHODS)
	    System.out.print(" METAMETHODS");
	if(SMART_CALL_GRAPH)
	    System.out.print(" SMART_CALL_GRAPH");
	if(!(METAMETHODS || SMART_CALL_GRAPH))
	    System.out.print(" DumbCallGraph");

	if(PointerAnalysis.CALL_CONTEXT_SENSITIVE)
	    System.out.print(" CALL_CONTEXT_SENSITIVE=" +
			     PointerAnalysis.MAX_SPEC_DEPTH);
	
	if(PointerAnalysis.THREAD_SENSITIVE)
	    System.out.print(" THREAD SENSITIVE");
	if(PointerAnalysis.WEAKLY_THREAD_SENSITIVE)
	    System.out.print(" WEAKLY_THREAD_SENSITIVE");

	if(PointerAnalysis.LOOP_SENSITIVE)
	    System.out.println(" LOOP_SENSITIVE");

	if(SHOW_CH)
	    System.out.print(" SHOW_CH");

	if(SHOW_CG)
	    System.out.print(" SHOW_CG");

	if(SHOW_DETAILS)
	    System.out.print(" SHOW_DETAILS");

	if(DO_ANALYSIS){
	    if(mm_to_analyze.size() == 1){
		Method method = (Method) (mm_to_analyze.iterator().next());
		System.out.println(" DO_ANALYSIS (" +
				   method.declClass + "." + method.name);
	    }
	    else{
		System.out.println("\nDO_ANALYSIS");
		for(Iterator it = mm_to_analyze.iterator(); it.hasNext();){
		    Method method = (Method) it.next();
		    System.out.println("  " + method.declClass + "." +
				       method.name);
		}
		System.out.println("}");
	    }
	}

	if(DO_INTERACTIVE_ANALYSIS)
	    System.out.print(" DO_INTERACTIVE_ANALYSIS");

	if(MA_MAPS){
	    System.out.print(" MA_MAPS (");
	    if(MAInfo.DO_METHOD_INLINING)
		System.out.print("inline; ");
	    System.out.print(MA_MAPS_OUTPUT_FILE + ")");
	}

	if(USE_INTER_THREAD)
	    System.out.print(" USE_INTER_THREAD");
	else
	    System.out.print(" (just inter proc)");

	if(DO_SAT)
	    System.out.print(" DO_SAT (" + SAT_FILE + ")");

	if(ELIM_SYNCOPS)
	    System.out.print(" ELIM_SYNCOPS");
	
	if(INST_SYNCOPS)
	    System.out.print(" INST_SYNCOPS");
	
	if(MAInfo.NO_TG)
	    System.out.println(" NO_TG");

	System.out.println();
    }


    private static boolean analyzable(MetaMethod mm) {
	HMethod hm = mm.getHMethod();
	if(java.lang.reflect.Modifier.isNative(hm.getModifiers()))
	    return false;
	return true;
    }

    // Generates the memory allocation policies: each allocation sites
    // is assigned one of the following allocation policy:
    // on the stack, on the thread specific heap or on the global heap.
    // The HcodeFactory (and implicitly the allocation pollicies maps as hcf
    // contains a pointer to the maps) and the linker are serialized into
    // a file.
    private static void ma_maps() {
	MetaCallGraph mcg = pa.getMetaCallGraph();
	MetaMethod mroot = new MetaMethod(hroot, true);
	Set allmms = mcg.getAllMetaMethods();

	// The following loop has just the purpose of timing the analysis of
	// the entire program. Doing it here, before any memory allocation
	// optimization, allows us to time it accurately.
       g_tstart = System.currentTimeMillis();
       for(Iterator it = allmms.iterator(); it.hasNext(); ) {
            MetaMethod mm = (MetaMethod) it.next();
            if(!analyzable(mm)) continue;
            pa.getIntParIntGraph(mm);
        }
        System.out.println("Intrathread Analysis time: " +
                           (time() - g_tstart) + "ms");

        if (USE_INTER_THREAD) {
          g_tstart = System.currentTimeMillis();
          for(Iterator it = allmms.iterator(); it.hasNext(); ) {
            MetaMethod mm = (MetaMethod) it.next();
            if(!analyzable(mm)) continue;
            pa.threadInteraction(mm);
          }
          System.out.println("Tnterthread Analysis time: " +
                           (time() - g_tstart) + "ms");
        }


	g_tstart = time();
	MAInfo mainfo = new MAInfo(pa, hcf, allmms, USE_INTER_THREAD);
	System.out.println("GENERATION OF MA INFO TIME  : " +
			   (time() - g_tstart) + "ms");

	if(SHOW_DETAILS) { // show the allocation policies	    
	    System.out.println();
	    mainfo.print();
	    System.out.println("===================================");
	}

	g_tstart = time();
	System.out.print("Dumping the code factory + maps into the file " +
			 MA_MAPS_OUTPUT_FILE + " ... ");
	try{
	    ObjectOutputStream oos = new ObjectOutputStream
		(new FileOutputStream(MA_MAPS_OUTPUT_FILE));
	    mainfo.prepareForSerialization();
	    // write the CachingCodeFactory on the disk
	    oos.writeObject(hcf);
	    // write the Linker on the disk
	    oos.writeObject(linker);
	    oos.flush();
	    oos.close();
	} catch(IOException e){ System.err.println(e); }
	System.out.println((time() - g_tstart) + "ms");
    }


    // One of my new ideas: while doing the intra-procedural analysis of
    // method M, instead of keeping a graph in each basic block, let's
    // keep one just for "join" points or for BB that contain a CALL.
    // The goal of this statistic is to see how many BBs fall in this category
    private static void join_stats(LBBConverter lbbconv, MetaCallGraph mcg) {

	System.out.println("\nPOTENTIAL MEMORY REDUCTION STATISTICS:\n");

	for(Iterator it = mcg.getAllMetaMethods().iterator(); it.hasNext();){
	    MetaMethod mm = (MetaMethod) it.next();
	    HMethod hm = mm.getHMethod();
	    if(Modifier.isNative(hm.getModifiers()))
		continue;
	    
	    LightBasicBlock.Factory lbbf = lbbconv.convert2lbb(hm);
	    LightBasicBlock lbbs[] = lbbf.getAllBBs();

	    int nb_lbbs = lbbs.length;
	    int nb_calls = 0; // nb. of blocks finished in CALLs
	    int nb_joins = 0; // nb. of blocks that are join points
	    int nb_total = 0; // nb. of blocks that are either CALLs or joins

	    for(int i = 0; i < nb_lbbs; i++){
		LightBasicBlock lbb = lbbs[i];
		boolean is_join = (lbb.getPrevLBBs().length > 1);

		HCodeElement elems[] = lbb.getElements();
		boolean is_call = (elems[elems.length - 1] instanceof CALL);

		if(is_call) nb_calls++;
		if(is_join) nb_joins++;
		if(is_call || is_join) nb_total++;
	    }

	    double pct = (float)(100 * nb_total)/(double)nb_lbbs;
	    HClass hc = hm.getDeclaringClass();
	    String method_name = hc.getName() + "." + hm.getName();

	    System.out.println(method_name + " \t" +
			       nb_lbbs  + " LBBs  " +
			       nb_total + " Full (" +
			       Debug.doubleRep(pct, 5, 2) + "%)  " +
			       nb_joins + " Joins " +
			       nb_calls + " Calls ");
	}
    }


    // Analyzes the methods given with the "-a" flag.
    private static void do_analysis() {
	for(Iterator it = mm_to_analyze.iterator(); it.hasNext(); ) {
	    Method analyzed_method = (Method) it.next();
	    if(analyzed_method.declClass == null)
		analyzed_method.declClass = root_method.declClass;
	    display_method(analyzed_method);
	}
    }


    // Analyzes the methods given interactively by the user.
    private static void do_interactive_analysis() {
	BufferedReader d = 
	    new BufferedReader(new InputStreamReader(System.in));
	while(true) {
	    System.out.print("Method name:");
	    String method_name = null;
	    try{
		method_name = d.readLine();
	    }catch(IOException e){}
	    if(method_name == null){
		System.out.println();
		break;
	    }
	    Method analyzed_method = getMethodName(method_name);
	    if(analyzed_method.declClass == null)
		analyzed_method.declClass = root_method.declClass;
	    try {
		display_method(analyzed_method);
	    }
	    catch(harpoon.ClassFile.NoSuchClassException e) {
		System.out.println("Class not found: \"" +
				   analyzed_method.declClass + "\"");
	    }
	}
    }

    private static void do_sat() {
	System.out.println(" Generating the \"start()\" and \"join()\" maps");
	System.out.println(" DUMMY VERSION");
	
	sat_starts = new HashSet();
	sat_joins  = new HashSet();
	
	for(Iterator it = mcg.getAllMetaMethods().iterator(); it.hasNext(); )
	    do_sat_analyze_mmethod((MetaMethod) it.next());

	System.out.println("Dumping the results into " + SAT_FILE);

	try{
	    ObjectOutputStream oos = new ObjectOutputStream
		(new FileOutputStream(SAT_FILE));

	    // write the CachingCodeFactory on the disk
	    System.out.print(" Dumping the code factory ... ");
	    oos.writeObject(hcf);
	    System.out.println("Done");

	    // write the Linker on the disk
	    System.out.print(" Dumping the linker ... ");
	    oos.writeObject(linker);
	    System.out.println("Done");

	    // write the set of .start() sites that need to be inlined
	    System.out.print(" Dumping the set of .start() ... ");
	    oos.writeObject(sat_starts);
	    System.out.println("Done");

	    // write the set of .join() sites that need to be modified
	    System.out.print(" Dumping the set of .join() ... ");
	    oos.writeObject(sat_joins);
	    System.out.println("Done");

	    oos.close();
	}
	catch(IOException e){
	    System.err.println(e);
	}
    }

    private static Set sat_starts = null;
    private static Set sat_joins  = null;

    private static QuadVisitor sat_qv = new QuadVisitor() {
	    public void visit(Quad q) { // do nothing
	    }
	    public void visit(CALL q) {
		HMethod method = q.method();
		if(isEqual(method, "java.lang.Thread", "start")) {
		    System.out.println("START: " + Debug.code2str(q));
		    sat_starts.add(q);
		}
		if(isEqual(method, "java.lang.Thread", "join") &&
		   (q.paramsLength() == 1)) {
		    System.out.println("JOIN: " + Debug.code2str(q));
		    sat_joins.add(q);
		}
	    }
	};
    
    // tests whether the method hm is the same thing as
    // class_name.method_name
    private static boolean isEqual(HMethod hm, String class_name,
				   String method_name) {
	HClass hclass = hm.getDeclaringClass();
	return(hm.getName().equals(method_name) &&
	       hclass.getName().equals(class_name));
    }

    private static void do_sat_analyze_mmethod(MetaMethod mm) {
	HMethod hm = mm.getHMethod();
	HCode hcode = hcf.convert(hm);
	if(hcode == null) return;
	for(Iterator it = hcode.getElementsI(); it.hasNext(); ) {
	    Quad q = (Quad) it.next();
	    q.accept(sat_qv);
	}
    }

    static void do_elim_syncops() {
	MetaCallGraph mcg = pa.getMetaCallGraph();
	MetaMethod mroot = new MetaMethod(hroot, true);
	Set allmms = mcg.getAllMetaMethods();
	SyncElimination se = new SyncElimination(pa);
	if (USE_INTER_THREAD)
	    se.addRoot_interthread(mroot);
	else
	    se.addRoot_intrathread(mroot);
	se.calculate();
	
	HCodeFactory hcf_nosync = SyncElimination.codeFactory(hcf, se);
	for(Iterator it = allmms.iterator(); it.hasNext(); ) {
            MetaMethod mm = (MetaMethod) it.next();
            HMethod m = mm.getHMethod();
            System.out.println("Eliminating Sync Ops in Method "+m);
	    HCode hcode = hcf_nosync.convert(m);
        }
    }

    static void do_elim_syncops(HMethod hm) {
	System.out.println("\nEliminating unnecessary synchronization operations.");
	
	SyncElimination se = new SyncElimination(pa);

    	for(Iterator it = split_rel.getValues(hm).iterator(); it.hasNext();){
	    MetaMethod mm = (MetaMethod) it.next();
	    if (USE_INTER_THREAD)
		se.addRoot_interthread(mm);
	    else
		se.addRoot_intrathread(mm);
	}
	
	se.calculate();
	
	HCodeFactory hcf_nosync = SyncElimination.codeFactory(hcf, se);
	
	//try {
	    java.io.PrintWriter out = new java.io.PrintWriter(System.out, true);
	    MetaCallGraph mcg = pa.getMetaCallGraph();
	    Set allmm = mcg.getAllMetaMethods();
	    Iterator it = allmm.iterator();
	    while (it.hasNext()) {
	        MetaMethod mm = (MetaMethod)it.next();
	        HMethod m = mm.getHMethod();
	        System.out.println("Transforming method "+m);
	        HCode hcode = hcf_nosync.convert(m);
	        if (hcode != null) hcode.print(out);
	    }
	//} catch (IOException x) {}

    }

    static void do_inst_syncops(HMethod hm) {
	System.out.println("\nInstrumenting synchronization operations.");
	
	InstrumentSyncOps se = new InstrumentSyncOps(pa);

    	for(Iterator it = split_rel.getValues(hm).iterator(); it.hasNext();){
	    MetaMethod mm = (MetaMethod) it.next();
	    se.addRoot(mm);
	}
	
	se.calculate();
	
	HCodeFactory hcf_instsync = InstrumentSyncOps.codeFactory(hcf, se);
	
	//try {
	    java.io.PrintWriter out = new java.io.PrintWriter(System.out, true);
	    MetaCallGraph mcg = pa.getMetaCallGraph();
	    Set allmm = mcg.getAllMetaMethods();
	    Iterator it = allmm.iterator();
	    while (it.hasNext()) {
	        MetaMethod mm = (MetaMethod)it.next();
	        HMethod m = mm.getHMethod();
	        System.out.println("Transforming method "+m);
	        HCode hcode = hcf_instsync.convert(m);
	        if (hcode != null) hcode.print(out);
	    }
	//} catch (IOException x) {}

    }
    
    static HClass[] get_classes(Set allmm) {
	HashSet ll = new HashSet();
	Iterator it = allmm.iterator();
	while (it.hasNext()) {
	    MetaMethod mm = (MetaMethod)it.next();
	    HMethod m = mm.getHMethod();
	    HClass hc = m.getDeclaringClass();
	    if (hc.isArray()) continue;
	    ll.add(hc);
	}
	return (HClass[])ll.toArray(new HClass[ll.size()]);
    }
    
    static void dump_java(HClass[] interfaceClasses) 
    throws IOException {

	java.io.PrintWriter out = new java.io.PrintWriter(System.out, true);
	for (int i=0; i<interfaceClasses.length; i++) {
	    HMethod hm1[] = interfaceClasses[i].getDeclaredMethods();
	    WorkSet hmo=new WorkSet();
	    System.out.println(interfaceClasses[i]+":");
	    for (int ind=0;ind<hm1.length;ind++) {
		hmo.add(hm1[ind]);
	    }
	    HMethod hm[] = new HMethod[hmo.size()];
	    Iterator hmiter=hmo.iterator();
	    int hindex=0;
	    while (hmiter.hasNext()) {
		hm[hindex++]=(HMethod)hmiter.next();
		System.out.println(hm[hindex-1]);
	    }

	    HCode hc[] = new HCode[hm.length];
	    HCodeFactory hcf2 = QuadWithTry.codeFactory(hcf);
	    for (int j=0; j<hm.length; j++) {
		hc[j] = hcf2.convert(hm[j]);
		if (hc[j]!=null) hc[j].print(out);
	    }
	    Jasmin jasmin=new Jasmin(hc, hm, interfaceClasses[i]);
	    FileOutputStream file;
	    if (interfaceClasses.length!=1)
		file=new FileOutputStream("out"+i+".j");
	    else
		file=new FileOutputStream("out.j");
	    PrintStream tempstream=new PrintStream(file);
	    jasmin.outputClass(tempstream);
	    file.close();
	}
    }

    private static String[] examples = {
	"java -mx200M harpoon.Main.PAMain -a multiplyAdd --ccs=2 --wts" + 
	"harpoon.Test.PA.Test1.complex",
	"java -mx200M harpoon.Main.PAMain -s -a run " + 
	"harpoon.Test.PA.Test2.Server",
	"java -mx200M harpoon.Main.PAMain -s " + 
	"-a harpoon.Test.PA.Test3.multisetElement.insert" + 
	" harpoon.Test.PA.Test3.multiset ",
	"java -mx200M harpoon.Main.PAMain -s -a sum " +
	"harpoon.Test.PA.Test4.Sum",
	"java harpoon.Main.PAMain -a foo harpoon.Test.PA.Test5.A"
    };

    private static String[] options = {
	"-m, --meta      Uses the real MetaMethods (unsupported yet).",
	"-s, --smart     Uses the SmartCallGrapph.",
	"-d, --dumb      Uses the simplest CallGraph (default).",
	"-c, --showch    Shows debug info about ClassHierrachy.",
	"--loadpre file  Loads the precomputed preanalysis results from disk.",
	"--savepre file  Saves the preanalysis results to disk.",
	"--showcg        Shows the (meta) call graph.",
	"--showsplit     Shows the split relation.",
	"--details       Shows details/statistics.",
	"--ccs=depth     Activates call context sensitivity with a given",
	"                 maximum call chain depth.",
	"--ts            Activates full thread sensitivity.",
	"--wts           Activates weak thread sensitivity.",
	"--ls            Activates loop sensitivity.",
	"--mamaps=file   Computes the allocation policy map and serializes",
	"                 the CachingCodeFactory (and implicitly the",
	"                 allocation map) and the linker to disk.",
	"-a method       Analyzes he given method. If the method is in the",
	"                 same class as the main method, the name of the",
	"                 class can be ommited. More than one \"-a\" flags",
	"                 can be used on the same command line.",
	"-i              Interactive analysis of methods.",
	"--noit          Just interprocedural analysis, no interthread.",
	"--inline        Use method inlining to enable more stack allocation",
	"                 (makes sense only with --mamaps).",
	"--sat=file      Generates dummy sets of calls to .start() and",
	"                 .join() that must be changed (for the thread",
	"                 inlining). Don't try to use it seriously!",
	"--notg          No thread group facility is necessary. In the",
	"                 future, this will be automatically detected by",
	"                 the analysis."
    };


    private static void show_help() {
	System.out.println("Usage:\n" +
	    "\tjava harpoon.Main.PAMain [options] <main_class>\n");
	
	System.out.println("Options:");
	for(int i = 0; i < options.length; i++)
	    System.out.println("\t" + options[i]);
	
	
	System.out.println("Examples:");
	for(int i = 0; i < examples.length; i++)
	    System.out.println("\t" + examples[i]);

	System.out.println("Suggestion:\n" +
	    "\tYou might consider the \"-mx\" flag of the JVM to satisfy\n" +
	    "\tthe huge memory requirements of the pointer analysis.\n" +
	    "Warning:\n\t\"Quite fast for small programs!\"" + 
	    " [Moolly Sagiv]\n" +
	    "\t\t... and only for them :-(");
    }


    // Constructs the class hierarchy of the analyzed program.
    private static void construct_class_hierarchy() {
	Set roots = new HashSet();
	roots.add(hroot);
	roots.addAll
	    (harpoon.Backend.Runtime1.Runtime.runtimeCallableMethods(linker));

	if(SHOW_CH){
	    System.out.println("Set of roots: {");
	    for(Iterator it = roots.iterator(); it.hasNext(); ){
		Object o = it.next();
		if(o instanceof HMethod)
		    System.out.println(" m: " + o);
		else
		    System.out.println(" c: " + o);
	    }
	    System.out.println("}");
	}

	System.out.print("ClassHierarchy ... ");
	long tstart = time();

	ch = new QuadClassHierarchy(linker, roots, hcf);

	System.out.println((time() - tstart) + "ms");

	if(SHOW_CH){
	    System.out.println("Root method = " + hroot);	    
	    System.out.println("Instantiated classes: {");
	    Set inst_cls = ch.instantiatedClasses();
	    for(Iterator it = inst_cls.iterator(); it.hasNext(); )
		System.out.println(" " + it.next());
	    System.out.println("}");
	}
    }


    // Returns the intersection of the set received as a parameter with
    // the set of methods.
    private static Set select_methods(Set set) {
	Set retval = new HashSet();
	for(Iterator it = set.iterator(); it.hasNext(); ) {
	    Object obj = it.next();
	    if(obj instanceof HMethod)
		retval.add(obj);
	}
	return retval;
    }


    // Returns the set of static initializers of all the instanciated classes.
    private static Set get_static_initializers() {
	Set retval = new HashSet();
	for(Iterator it = ch.classes().iterator(); it.hasNext(); ) {
	    HClass hclass = (HClass) it.next();
	    HMethod hm = hclass.getClassInitializer();
	    if(hm != null)
		retval.add(hm);
	}
	return retval;
    }


    // Constructs the set of method roots (see the comments around mroots)
    private static void construct_mroots() {
	mroots = new HashSet();
	mroots.addAll(select_methods(runtime_callable));
	mroots.addAll(get_static_initializers());
	mroots.add(hroot);

	if(SHOW_DETAILS) {
	    System.out.println("Method roots: {");
	    for(Iterator it = mroots.iterator(); it.hasNext(); )
		System.out.println(" " + (HMethod) it.next());
	    System.out.println("}");
	}
    }


    // Constructs the meta call graph and the meta all callers objects
    // for the currently analyzed program.
    private static void construct_meta_call_graph() {
	long tstart = 0L;

	if(METAMETHODS){ // real meta-methods
	    System.out.print("MetaCallGraph ... ");
	    tstart = time();
	    mcg = new MetaCallGraphImpl(bbconv, ch, mroots);
	    System.out.println((time() - tstart) + "ms");

	    System.out.print("MetaAllCallers ... ");
	    tstart = time();
	    mac = new MetaAllCallers(mcg);
	    System.out.println((time() - tstart) + "ms");
	}
	else{
	    // the set of "run()" methods (the bodies of threads)
	    Set run_mms = null;
	    CallGraph cg = null;

	    if(SMART_CALL_GRAPH){ // smart call graph!
		System.out.print("MetaCallGraph ... ");
		tstart = time();
		MetaCallGraph fmcg = new MetaCallGraphImpl(bbconv, ch, mroots);
		System.out.println((time() - tstart) + "ms");

		run_mms = fmcg.getRunMetaMethods();

		System.out.print("SmartCallGraph ... ");
		tstart = time();
		cg = new SmartCallGraph(fmcg);
		System.out.println((time() - tstart) + "ms");
	    }
	    else
		cg = new CallGraphImpl(ch, hcf);

	    System.out.print("FakeMetaCallGraph ... ");
	    tstart = time();
	    mcg = new FakeMetaCallGraph(cg, cg.callableMethods(), run_mms);
	    System.out.println((time() - tstart) + "ms");
	    
	    System.out.print("(Fake)MetaAllCallers ... ");
	    tstart = time();
	    mac = new MetaAllCallers(mcg);
	    System.out.println((time() - tstart) + "ms");
	}

	if(SHOW_CG){
	    System.out.println("MetaCallGraph:");
	    mcg.print(new java.io.PrintWriter(System.out, true), true,
		      new MetaMethod(hroot, true));
	}
    }


    // Constructs the split relation attached to the current meta call graph.
    private static void construct_split_relation() {
	System.out.print("SplitRelation ... ");
	long tstart = time();
	split_rel = mcg.getSplitRelation();
	System.out.println((time() - tstart) + "ms");

	if(SHOW_SPLIT){
	    System.out.println("Split relation:");
	    Debug.show_split(mcg.getSplitRelation());
	}
    }


    private static long time() {
	return System.currentTimeMillis();
    }
}
