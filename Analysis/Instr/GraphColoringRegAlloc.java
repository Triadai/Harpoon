// GraphColoringRegAlloc.java, created Mon Jul 17 16:39:13 2000 by pnkfelix
// Copyright (C) 2000 Felix S. Klock <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.Instr;

import harpoon.Analysis.Maps.Derivation;
import harpoon.Analysis.DataFlow.LiveTemps;
import harpoon.Analysis.ReachingDefs;
import harpoon.Analysis.ReachingDefsAltImpl;
import harpoon.Analysis.GraphColoring.AbstractGraph;
import harpoon.Analysis.GraphColoring.ColorableGraph;
import harpoon.Analysis.GraphColoring.Color;
import harpoon.Analysis.GraphColoring.GraphColorer;
import harpoon.Analysis.GraphColoring.SimpleGraphColorer;
import harpoon.Analysis.GraphColoring.UnableToColorGraph;
import harpoon.Backend.Generic.Code;
import harpoon.Backend.Generic.RegFileInfo;
import harpoon.IR.Assem.Instr;
import harpoon.Temp.Temp;
import harpoon.Util.Util;
import harpoon.Util.CombineIterator;
import harpoon.Util.FilterIterator;
import harpoon.Util.Default;
import harpoon.Util.ArraySet;
import harpoon.Util.Collections.ListFactory;
import harpoon.Util.Collections.Factories;
import harpoon.Util.Collections.LinearSet;
import harpoon.Util.Collections.MultiMap;
import harpoon.Util.Collections.GenericMultiMap;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.List;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.Collections;

/**
 * <code>GraphColoringRegAlloc</code> uses graph coloring heuristics
 * to find a register assignment for a Code.
 * 
 * @author  Felix S. Klock <pnkfelix@mit.edu>
 * @version $Id: GraphColoringRegAlloc.java,v 1.1.2.10 2000-07-28 21:49:14 pnkfelix Exp $
 */
public class GraphColoringRegAlloc extends RegAlloc {
    
    public static RegAlloc.Factory FACTORY =
	new RegAlloc.Factory() {
	    public RegAlloc makeRegAlloc(Code c) {
		return new GraphColoringRegAlloc(c);
	    }
	};

    private static final int INITIAL_DISPLACEMENT = 0;

    /* ** Fields ** */
    
    double defWt, useWt, copyWt;
    int baseReg;
    int disp = INITIAL_DISPLACEMENT, argReg;
    List stack; // List<Integer>
    Map realReg; // Integer -> Integer

    final RegFileInfo rfi;
    ReachingDefs rdefs;
    LiveTemps liveTemps;

    MultiMap regToDefs;
    MultiMap regToUses;
    

    List regAssigns; // List< List<Temp> >  
    Map regToColor;  // Temp -> RegColor

    Map ixtToWeb; // Instr x Temp -> WebRecord
    List webRecords; // List<WebRecord>

    List tempWebRecords; // List<TempWebRecord>
    List assignWebRecords; // List<AssignWebRecord>
    
    // Maps Temp:t -> Set of Regs whose live regions interfere with
    //                t's live region
    MultiMap preassignMap;

    /** Creates a <code>GraphColoringRegAlloc</code>. */
    public GraphColoringRegAlloc(Code code) {
        super(code);
	rfi = frame.getRegFileInfo();
	buildRegAssigns();
	rdefs = new ReachingDefsAltImpl(code);
	liveTemps = LiveTemps.make(code, rfi.liveOnExit());
	preassignMap = buildPreassignMap(code, rfi.liveOnExit());
    }

    protected Derivation getDerivation() {
	return null;
    }

    protected MultiMap buildPreassignMap(Code code, Set liveOnExit) {
	return null;
    }

    /** Returns a new Collection cr, where
	for all e element of edges 
	   there exists a unique c element of cr such that 
	   c = [nodeToNum(e.get(0)), nodeToNum(e.get(1))]
    */
    private Collection readableEdges(final Collection edges, 
				     final Map nodeToNum) {
	return new AbstractCollection() {
	    public int size() { return edges.size(); }
	    public Iterator iterator() { 
		return new FilterIterator 
		    (edges.iterator(),
		     new FilterIterator.Filter() {
			public Object map(Object o) {
			    List l=(List)o;
			    return Default.pair(nodeToNum.get(l.get(0)),
						nodeToNum.get(l.get(1)));
			}});
	    }
	};
    }

    protected void generateRegAssignment() {
	boolean success, coalesced;
	AdjMtx adjMtx;

	GraphColorer colorer = new SimpleGraphColorer();

	do {
	    do {
		makeWebs(rdefs); 
		
		// System.out.println("webs: "+webRecords);

		adjMtx = buildAdjMatrix();
		// System.out.println("Adjacency Matrix");
		// System.out.println(adjMtx);
		coalesced = coalesceRegs(adjMtx);
	    } while (coalesced);

	    WebRecord[] adjLsts = buildAdjLists(adjMtx); 
	    adjMtx = null;

	    // System.out.println(Arrays.asList(adjLsts));

	    computeSpillCosts();

	    final ColorableGraph graph = new Graph(adjLsts);
	    
	    final Map nodeToNum = new HashMap(); 
	    System.out.println("nodes of graph");
	    int i=0;
	    for(Iterator nodes=graph.nodeSet().iterator();nodes.hasNext();){
		i++;
		Object n=nodes.next();
		nodeToNum.put(n, new Integer(i));
		System.out.println(i+"\t"+n);
	    }
	    Collection readEdges = readableEdges(graph.edges(), nodeToNum);;
	    System.out.println("edges of graph "+readEdges);

	    try {
		List colors = new ArrayList(regToColor.values());
		// System.out.println("colors:"+colors);
		colorer.color(graph, colors);
		
		MultiMap c2n = new GenericMultiMap();
		for(Iterator nds=graph.nodeSet().iterator();nds.hasNext();){
		    Object nd = nds.next();
		    c2n.add(graph.getColor(nd), nd);
		}
		for(Iterator cs=c2n.keySet().iterator();cs.hasNext();){
		    Object col=cs.next();
		    System.out.println(col + " nodes: "+c2n.getValues(col));
		}

		modifyCode();
		
		success = true;
	    } catch (UnableToColorGraph e) {
		success = false;

		System.out.println("Unable to color graph");
		System.out.println();
		System.out.println("edges of graph "+
				   readableEdges(graph.edges(),nodeToNum));
		System.exit(-1);

		genSpillCode();

	    }
	} while (!success);
    }

    // sets regAssigns, regToColor, regToDefs, regToUses
    private void buildRegAssigns() {
	HashSet assigns = new HashSet();
	regToColor = new HashMap();

	regToDefs = new GenericMultiMap();
	regToUses = new GenericMultiMap();

	for(Iterator instrs=code.getElementsI(); instrs.hasNext();){
	    Instr i = (Instr) instrs.next();
	    Iterator tmps = new CombineIterator(i.defC().iterator(),
						i.useC().iterator());
	    while(tmps.hasNext()) {
		Temp t = (Temp) tmps.next();
		if (rfi.isRegister(t)) {
		    if (i.defC().contains(t)) regToDefs.add(t, i);
		    if (i.useC().contains(t)) regToUses.add(t, i);
		    // regToColor(t); 
		} else {
		    Set suggRegs = rfi.getRegAssignments(t);
		    assigns.addAll(suggRegs);
		    for(Iterator s=suggRegs.iterator(); s.hasNext();){
			List rL = (List) s.next();
			for(Iterator rs=rL.iterator();rs.hasNext();){
			    Temp reg = (Temp) rs.next();
			    regToColor(reg); 
			}
		    }
		}
	    }
	}
	regAssigns = new ArrayList(assigns);
    }
    
    private Color regToColor(Temp reg) {
	Color c = (Color) regToColor.get(reg);
	if (c == null) {
	    c = new RegColor(reg);
	    regToColor.put(reg, c);
	}
	return c;
    }

    class RegColor extends Color {
	final Temp reg;
	RegColor(Temp r) {
	    this.reg = r;
	}
	public String toString() { 
	    return "c:"+reg;
	}
    }
    
    /**
       nwebs is set after this method returns.
       assignWebRecords, tempWebRecords, and webRecords are set
       after this method returns.
     */
    private void makeWebs(ReachingDefs rdefs) {
	Set webSet = new HashSet(), tmp1, tmp2; // Set<TempWebRecord>
	TempWebRecord web1, web2;
	List sd; // [Temp, Def]
	int i, oldnwebs;
	
	
	for(Iterator instrs = code.getElementsI();instrs.hasNext();){ 
	    Instr inst = (Instr) instrs.next();
	    for(Iterator uses = inst.useC().iterator(); uses.hasNext();){ 
		Temp t = (Temp) uses.next();
		if (isRegister(t)) continue;

		TempWebRecord web = 
		    new TempWebRecord
		    (t, new LinearSet(rdefs.reachingDefs(inst,t)),
		     new LinearSet(Collections.singleton(inst)));
		webSet.add(web);
	    }

	    // in practice, the remainder of the web building should
	    // work W/O the following loop.  but if there is a def w/o
	    // a corresponding use, the system breaks.
	    for(Iterator defs = inst.defC().iterator(); defs.hasNext();){
		Temp t = (Temp) defs.next();
		if (isRegister(t)) continue;

		TempWebRecord web =
		    new TempWebRecord
		    (t, new LinearSet(Collections.singleton(inst)),
		     new LinearSet(Collections.EMPTY_SET));
		webSet.add(web);
	    }
	}

	// System.out.println("pre-duchain-combination");
	// System.out.println("webSet: "+webSet);

	boolean changed;
	do {
	    // combine du-chains for the same symbol and that have a
	    // def in common to make webs  
	    changed = false;
	    tmp1 = new HashSet(webSet);
	    while(!tmp1.isEmpty()) {
		web1 = (TempWebRecord) tmp1.iterator().next();
		tmp1.remove(web1);
		tmp2 = new HashSet(tmp1);
		while(!tmp2.isEmpty()) {
		    web2 = (TempWebRecord) tmp2.iterator().next();
		    tmp2.remove(web2);
		    if (web1.sym.equals(web2.sym)) {
			boolean combineWebs;
			Set ns = new HashSet(web1.defs);
			ns.retainAll(web2.defs);
			combineWebs = !ns.isEmpty();
			
			if (!combineWebs) {
			    // IMPORTANT: current temp->reg assignment
			    // design breaks if an instr needs two
			    // different regs for the same temp in the
			    // uses and defines.  Take these out after that
			    // is fixed. 
			    Set s1 = new HashSet(web1.defs);
			    s1.retainAll(web2.uses);
			    
			    Set s2 = new HashSet(web2.defs);
			    s2.retainAll(web1.uses);
			    combineWebs = (!s1.isEmpty() || !s2.isEmpty());
			}
			
			
			if (combineWebs) {
			    web1.defs.addAll(web2.defs);
			    web1.uses.addAll(web2.uses);
			    webSet.remove(web2);
			    changed = true;
			}
			
		    }
		}
	    }
	} while ( changed );
	
	// System.out.println("post-duchain-combination");
	// System.out.println("webSet: "+webSet);
	
	// FSK: may need to switch the thinking here from "number of
	// regs" to "number of possible assignments" which is a
	// different beast altogether...
	
	assignWebRecords = new ArrayList(regAssigns.size());
	for(i=0; i < regAssigns.size(); i++) {
	    WebRecord w = new AssignWebRecord((List)regAssigns.get(i));
	    w.sreg(i);
	    assignWebRecords.add(w);
	}
	tempWebRecords = new ArrayList(webSet.size());
	for(Iterator webs = webSet.iterator(); webs.hasNext(); i++) {
	    WebRecord w = (TempWebRecord) webs.next();
	    w.sreg(i);
	    tempWebRecords.add(w);
	}
	webRecords = ListFactory.concatenate
	    (Default.pair(assignWebRecords, tempWebRecords));
    }

    private AdjMtx buildAdjMatrix() { 
	AdjMtx adjMtx = new AdjMtx(webRecords);
	int i, j;

	Iterator assgn1 = assignWebRecords.iterator();
	while(assgn1.hasNext()) {
	    AssignWebRecord awr1 = (AssignWebRecord) assgn1.next();
	    Iterator assgn2 = assignWebRecords.iterator();
	    while(assgn2.hasNext()) {
		AssignWebRecord awr2 = (AssignWebRecord)assgn2.next();
		if (awr1 == awr2) 
		    break;
		HashSet regs = new HashSet(awr1.regs);
		regs.removeAll(awr2.regs);
		adjMtx.set(awr1.sreg(),awr2.sreg(),!regs.isEmpty());
	    }
	}
	for(i=1; i<webRecords.size(); i++) {
	    for(j=0; j<i; j++) {
		WebRecord wr1 = (WebRecord) webRecords.get(i);
		WebRecord wr2 = (WebRecord) webRecords.get(j);
		adjMtx.set(i,j, wr1.conflictsWith(wr2));
	    }
	}
	return adjMtx;
    }
    
    // This '.left' stuff is bullshit... just a complicated way of
    // indicating the definition type and doing the necessary
    // replacement... temp remapping should look cleaner...
    private boolean coalesceRegs(AdjMtx adjMtx) { 
	return false;
	/*
	int i, j, k, l, p, q;
	Instr inst, pqinst;
	for(i=1; i<=nblocks; i++) {
	    for(j=1; j<=ninsts[i]; j++) {
		inst = LBlock[i][j];
		if (inst.kind = regval) {
		    k = Reg_to_Int(inst.left);
		    l = Reg_to_Int(inst.opd.val);
		    if (! adjMtx.get(k,l) ||
			nonStore(LBlock,k,l,i,j)) {
			for(p=1; p<nblocks; p++) {
			    for(q=1; q<ninsts[p]; q++) {
				pqinst = LBlock[p][q];
				if (LIR_Has_Left(pqinst) &&
				    pqinst.left == inst.opt.val) {
				    pqinst.left = inst.left;
				}
			    }
			}
		    }
		    // remove the copy instruction 
		    inst.remove();
		    ((WebRecord)symReg.get(k)).defs
			.addAll(((WebRecord)symReg.get(l)).defs);
		    ((WebRecord)symReg.get(k)).uses
			.addAll(((WebRecord)symReg.get(l)).uses);
		    symReg.set(1, symReg.get(nwebs));
		    for(p=1; p<=nwebs; p++) {
			if (adjMtx.get(p,l)) {
			    adjMtx.set(p,l,true);
			}
			adjMtx.set(p,l, adjMtx.get(nwebs,p));
		    }
		    nwebs--;
		}
	    }
	}
	*/
    }

    private WebRecord[] buildAdjLists(AdjMtx adjMtx) { 
	int i, j;
	final int nwebs = webRecords.size();
	final WebRecord[] adjLsts = new WebRecord[nwebs];
	for(i=0; i<assignWebRecords.size(); i++) {
	    adjLsts[i] = (WebRecord) assignWebRecords.get(i);
	    adjLsts[i].spcost =  Double.POSITIVE_INFINITY;
	}
	int offset = assignWebRecords.size();
	for(i=0; i<tempWebRecords.size(); i++) {
	    adjLsts[offset+i]= (WebRecord) tempWebRecords.get(i);
	}
	for(i=1; i < nwebs; i++) {
	    for(j=0; j < i; j++) {
		if (adjMtx.get(i,j)) {
		    adjLsts[i].adjnds.add(adjLsts[j]);
		    adjLsts[j].adjnds.add(adjLsts[i]);
		    adjLsts[i].nints++;
		    adjLsts[j].nints++;
		}
	    }
	}
	return adjLsts;
    }

    private void computeSpillCosts() { 
	
    }

    private void pruneGraph() { 
    
    }

    private boolean assignRegs() { 
	return true; 
    }

    private void modifyCode() { 
	MultiMap colorToAssign; // RegColor -> List<Temp>
	colorToAssign = new GenericMultiMap();
	for(Iterator ars = assignWebRecords.iterator(); ars.hasNext();){
	    AssignWebRecord wr = (AssignWebRecord) ars.next();
	    colorToAssign.add(wr.regColor(), wr.regs);
	}
	for(Iterator wrs = tempWebRecords.iterator(); wrs.hasNext();){
	    TempWebRecord wr = (TempWebRecord) wrs.next();
	    Iterator instrs;
	    for(instrs = wr.defs.iterator(); instrs.hasNext();) {
		Instr i = (Instr) instrs.next();
		List regs =  (List) colorToAssign.get(wr.regColor());
		Util.assert(regs != null);
		code.assignRegister(i, wr.sym, regs);
	    }
	    for(instrs = wr.uses.iterator(); instrs.hasNext();) {
		Instr i = (Instr) instrs.next();
		List regs =  (List) colorToAssign.get(wr.regColor());
		Util.assert(regs != null);
		code.assignRegister(i, wr.sym, regs);
	    }
	}
    } 

    private void genSpillCode() { 

    }
    
    /** Graph is a graph view of the adjacency lists in this. 
	Every element of a Graph is a WebRecord.
     */
    class Graph extends AbstractGraph implements ColorableGraph {
	LinkedList adjLsts;
	LinkedList hidden;
	Graph(WebRecord[] adjLsts) {
	    this.adjLsts = new LinkedList(Arrays.asList(adjLsts));
	    hidden = new LinkedList();
	}
	public Set nodeSet() { 
	    return new AbstractSet() {
		public int size() { return adjLsts.size(); }
		public Iterator iterator() {
		    return adjLsts.iterator();
		}
	    };
	}
	public Collection neighborsOf(Object n) { 
	    if (!(n instanceof WebRecord))
		throw new IllegalArgumentException();
	    WebRecord lr = (WebRecord) n;
	    return lr.adjnds;
	}
	public void resetGraph() { replaceAll(); resetColors(); }
	public void hide(Object n) { 
	    if (adjLsts.remove(n)) { // check if in nodeSet
		WebRecord lr = (WebRecord) n;
		Iterator nbors;
		for(nbors=lr.adjnds.iterator(); nbors.hasNext();){ 
		    WebRecord nbor = (WebRecord) nbors.next();
		    boolean changed = nbor.adjnds.remove(lr);
		    Util.assert(changed);
		}
		hidden.addLast(lr);
	    } else {
		throw new IllegalArgumentException();
	    }
	}
	public Object replace() { 
	    WebRecord lr;
	    try {
		lr = (WebRecord) hidden.removeLast();
		adjLsts.add(lr);
		for(Iterator nbors=lr.adjnds.iterator();nbors.hasNext();){ 
		    WebRecord nbor = (WebRecord) nbors.next();
		    nbor.adjnds.add(lr);
		}
	    } catch (java.util.NoSuchElementException e) {
		lr = null;
	    }
	    return lr;
	}
	public void replaceAll() {
	    while(!hidden.isEmpty()) {
		replace();
	    }
	}
	public Color getColor(Object n) { 
	    if (adjLsts.contains(n)) {
		WebRecord lr = (WebRecord) n;
		return lr.regColor();
	    } else {
		throw new IllegalArgumentException();
	    }
	}

	// FSK: not implemented correctly; does not support node
	// precoloring. 
	public void resetColors() { 
	    Iterator ns;
	    for(ns = adjLsts.iterator(); ns.hasNext();) {
		((WebRecord) ns.next()).regColor(null);
	    }
	    for(ns = hidden.iterator(); ns.hasNext();) {
		((WebRecord) ns.next()).regColor(null);
	    }
	}

	public void setColor(Object n, Color c) { 
	    try {
		((WebRecord)n).regColor((RegColor) c);
	    } catch (ClassCastException e) {
		throw new IllegalArgumentException();
	    }
	}
    }

    abstract class WebRecord {
	int nints, disp;
	double spcost;
	List adjnds; // List<WebRecord>

	private int sreg; 
	private boolean sregYet = false;

	private RegColor regColor;
	private boolean regColorYet = false;
	final boolean colorOnce;
      
	WebRecord(boolean colorOnce) {
	    nints = 0;
	    regColor = null;
	    disp = Integer.MIN_VALUE;
	    spcost = 0.0;
	    adjnds = new LinkedList();
	    this.colorOnce = colorOnce;
	}
	
	int sreg() { Util.assert(sregYet); return sreg; }
	void sreg(int val) {
	    Util.assert(!sregYet);
	    sreg = val;
	    sregYet = true;
	}

	RegColor regColor()  {
	    return regColor;
	}
	
	void regColor(RegColor rc)  {
	    if (colorOnce && regColorYet) {
		// attempts to erase color should do nothing 
		Util.assert(rc == null);
	    } else {
		regColor = rc;
		regColorYet = true;
	    }
	}
	
	
	// ( interference based on Muchnick, page 494.)
	boolean conflictsWith(WebRecord wr) {
	    return this.conflictsWith1D(wr) ||
		wr.conflictsWith1D(this);
	}

	// one directional conflict check (helper function) 
	// if this is live at a def in wr, returns true.
	private boolean conflictsWith1D(WebRecord wr) {
	    for(Iterator tmps=wr.temps().iterator();tmps.hasNext();){
		Temp t = (Temp) tmps.next();
		for(Iterator ins=this.defs().iterator();ins.hasNext();){
		    Instr d = (Instr) ins.next();
		    Set l = liveTemps.getLiveAfter(d);
		    if (l.contains(t)) 
			return true;
		}
	    }
	    return false;
	}
	private boolean oldConflictsWith1D(WebRecord wr) {
	    for(Iterator tmps=wr.temps().iterator();tmps.hasNext();){
		Temp t = (Temp) tmps.next();
		for(Iterator ins=this.uses().iterator();ins.hasNext();){
		    Instr u = (Instr) ins.next();
		    if (isRegister(t) &&
			!regToDefs.keySet().contains(t)) {
			// not all regs are explicitly defined; doing
			// queries on them breaks rdefs.
			continue;
		    }
		    Set reaches = rdefs.reachingDefs(u, t);
		    reaches = new HashSet(reaches); // make mutable
		    reaches.retainAll(wr.defs());
		    if (!reaches.isEmpty()) {
			System.out.println(this+" conflicts with\n"+wr+
					   "\ndue to "+reaches+
					   "\n reaching "+u);
			System.out.println();
			return true;
		    }
		}
	    }
	    return false;
	}

	// returns the set of instrs that this web holds definitions
	// for.  These instrs are used to detect conflicts between
	// webs. 
	abstract Set defs();

	// returns the set of instrs that this web holds uses
	// for.  These instrs are used to detect conflicts between
	// webs. 
	abstract Set uses();

	abstract List temps();
    }

    class AssignWebRecord extends WebRecord {
	List regs;
	AssignWebRecord(List regs) {
	    super(regs.size() == 1);
	    this.regs = regs;
	    if (regs.size() == 1) {
		regColor( (RegColor)regToColor.get(regs.get(0)) );
	    } else {
		System.out.println("!!! Created record for "+regs);
	    }
	}
	public List temps() { return regs; }
	public Set defs() { 
	    HashSet s = new HashSet();
	    for(Iterator ts=regs.iterator(); ts.hasNext();){
		Temp reg = (Temp) ts.next();
		s.addAll(regToDefs.getValues(reg));
	    }
	    return s;
	}

	public Set uses() { 
	    HashSet s = new HashSet();
	    for(Iterator ts=regs.iterator(); ts.hasNext();){
		Temp reg = (Temp) ts.next();
		s.addAll(regToUses.getValues(reg));
	    }
	    return s;
	}

	boolean conflictsWith(WebRecord wr) {
	    if (wr instanceof AssignWebRecord) {
		AssignWebRecord awr = (AssignWebRecord) wr;
		HashSet r = new HashSet(regs);
		r.retainAll(awr.regs);
		return r.isEmpty();
	    } else {
		return super.conflictsWith(wr);
	    }
	}
	public String toString() {
	    return "w:"+regs;
	}
    }


    // converts a Set<Instr>:s1 into a Set<Integer>:s2 where the
    // elements in s2 correspond to the IDs of the instructions in s1
    private static Integer i2int(Instr i) { return new Integer(i.getID()); }
    private static Set readable(final Set instrs) {
	final FilterIterator.Filter fltr = new FilterIterator.Filter() {
	    public Object map(Object o) { return i2int((Instr)o); }
	};
	return new AbstractSet() {
	    public int size() { return instrs.size(); }
	    public Iterator iterator() {
		return new FilterIterator(instrs.iterator(), fltr);
	    }
	};
    }

    class TempWebRecord extends WebRecord {
	Temp sym;
	Set defs, uses; // Set<Instr>
	boolean spill;
	int disp;
	
	TempWebRecord(Temp symbol, Set defSet, Set useSet) {
	    super(false);
	    sym = symbol; defs = defSet; uses = useSet;
	    spill = false; disp = -1;
	}
	
	public List temps() { return Collections.nCopies(1, sym); }
	public Set defs() { return Collections.unmodifiableSet(defs);
	}
	public Set uses() { return Collections.unmodifiableSet(uses); }


	public String toString() {
	    if (true) 
		return "< sym:"+sym+
		    ", defs:"+readable(defs)+
		    ", uses:"+readable(uses)+
		    // ", spill:"+spill+", sreg:"+sreg+
		    // ", disp:"+disp+
		    " >";
	    else 
		return "w:"+sym;
	}
    }

    class OpdRecord {
	boolean isVar() { return false; }
	boolean isRegno() { return false; }
	boolean isConst() { return false; }
	Temp val;
    }

    class AdjMtx {
	// implement here: a Lower Triangular Matrix backed by a
	// BitString.  Note that for Lower Triangular Matrix, order of
	// coordinate args is insignificant (from p.o.v. of user).

	private class IntPairSet {
	    final int x, y; // Invariant: x < y
	    IntPairSet(int x, int y) {
		Util.assert(x != y);
		if (x < y) {
		    this.x = x;
		    this.y = y;
		} else {
		    this.x = y;
		    this.y = x;
		}
	    }
	    public int hashCode() {
		return (x << 16) ^ y;
	    }
	    public boolean equals(Object o) {
		IntPairSet p = (IntPairSet) o;
		return (p.x == x) && (p.y == y);
	    }
	    public String toString() {
		// return "<"+x+","+y+">";
		return "<"+symReg.get(x)+","+symReg.get(y)+">";
	    }
	}

	final List symReg;

	final HashSet back;

	/** Constructs a (symReg.size x symReg.size / 2) Lower
	    Triangular Adjacency Matrix. 
	    <BR> <B>requires:</B> symReg is a List<WebRecord>
	    All values map to false at construction time.
	*/ 
	AdjMtx(List symReg) { 
	    back = new HashSet();
	    this.symReg = symReg;
	}

	boolean get(int x, int y) { 
	    return back.contains(new IntPairSet(x, y));
	}
	void set(int x, int y, boolean b) {
	    if (b) {
		back.add(new IntPairSet(x, y));
	    } else {
		back.remove(new IntPairSet(x, y));
	    }
	}

	public String toString() {
	    return back.toString();
	}
    }
       
    
}
