// ReachingDefsImpl.java, created Wed Feb  9 16:35:43 2000 by kkz
// Copyright (C) 2000 Karen K. Zee <kkz@tesuji.lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis;

import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;
import harpoon.IR.Properties.CFGrapher;
import harpoon.IR.Properties.UseDef;
import harpoon.Temp.Temp;
import harpoon.Util.Collections.BitSetFactory;
import harpoon.Util.Worklist;
import harpoon.Util.WorkSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
/**
 * <code>ReachingDefsImpl</code> defines an implementation
 * for analyzing reaching definitions. Since results are
 * cached, a new <code>ReachingDefsImpl</code> should be
 * created if the code has been modified.
 * 
 * @author  Karen K. Zee <kkz@tesuji.lcs.mit.edu>
 * @version $Id: ReachingDefsImpl.java,v 1.1.2.1 2000-02-10 01:54:02 kkz Exp $
 */
public class ReachingDefsImpl extends ReachingDefs {
    final private CFGrapher cfger;
    final private BasicBlock.Factory bbf;
    /** Creates a <code>ReachingDefsImpl</code> object for the
	provided <code>HCode</code> using the provided 
	<code>CFGrapher</code>. This may take a while since the
	analysis is done at this time.
    */
    public ReachingDefsImpl(HCode hc, CFGrapher cfger) {
	super(hc);
	this.cfger = cfger;
	this.bbf = new BasicBlock.Factory(hc.getRootElement(), cfger);
	analyze();
    }
    /** Creates a <code>ReachingDefsImpl</code> object for the
	provided <code>HCode</code>. Uses <code>CFGrapher.DEFAULT</code>.
    */
    public ReachingDefsImpl(HCode hc) {
	this(hc, CFGrapher.DEFAULT);
    }
    /** Returns the Set of <code>HCodeElement</code>s providing definitions
     *  of <code>Temp</code> <code>t</code> which reach 
     *  <code>HCodeElement</code> <code>hce</code>. */
    public Set reachingDefs(HCodeElement hce, Temp t) {
	// find out which BasicBlock this HCodeElement is from
	BasicBlock b = bbf.getBlock(hce);
	// get the map for the BasicBlock
	Map m = (Map)cache.get(b);
	// get the BitSetFactory
	BitSetFactory bsf = (BitSetFactory)Temp_to_BitSetFactories.get(t);
	// make a copy of the in Set for the BasicBlock
	Set results = bsf.makeSet((Set)m.get(t));
	// go through the HCodeElements in order
	for(Iterator it=b.statements().iterator(); it.hasNext(); ) {
	    HCodeElement curr = (HCodeElement)it.next();
	    if (curr == hce) break;
	    if (((UseDef)curr).defC().contains(t)) 
		results = bsf.makeSet(Collections.singleton(curr));
	}
	return results;
    }
    final private Map Temp_to_BitSetFactories = new HashMap();
    // do analysis
    private void analyze() {
	final Map Temp_to_DefPts = getDefPts();
	getBitSets(Temp_to_DefPts);
	
	// build Gen and Kill sets
	buildGenKillSets(Temp_to_DefPts);
	// solve for fixed point
	solve();
	// store only essential information
	for(Iterator it=cache.keySet().iterator(); it.hasNext(); ) {
	    BasicBlock b = (BasicBlock)it.next();
	    Map m = (Map)cache.get(b);
	    for(Iterator temps=m.keySet().iterator(); temps.hasNext(); ) {
		Temp t = (Temp)temps.next();
		Set[] results = (Set[])m.get(b);
		m.put(b, results[IN]);
	    }
	}
    }
    final Map cache = new HashMap(); // maps BasicBlocks to in Sets 
    // return a mapping of BasicBlocks to a mapping of Temps to
    // an array of bitsets where the indices are organized as follows:
    // 0 - gen Set
    // 1 - kill Set
    // 2 - in Set
    // 3 - out Set
    private Map buildGenKillSets(Map DefPts) {
	Map m = new HashMap();
	// calculate Gen and Kill sets
	for(Iterator blocks=bbf.blockSet().iterator(); blocks.hasNext(); ) {
	    BasicBlock b = (BasicBlock)blocks.next();
	    Map Temp_to_BitSets = new HashMap();
	    for(Iterator it=b.statements().iterator(); it.hasNext(); ) {
		HCodeElement hce = (HCodeElement)it.next();
		Temp[] tArray = ((UseDef)hce).def();
		for(int i=0; i < tArray.length; i++) {
		    Temp t = tArray[i];
		    BitSetFactory bsf 
			= (BitSetFactory)Temp_to_BitSetFactories.get(t);
		    Set[] bitSets = new Set[4]; // 0 is Gen, 1 is Kill
		    bitSets[GEN] = bsf.makeSet(Collections.singleton(hce));
		    Set kill = new HashSet((Set)DefPts.get(t));
		    kill.remove(hce);
		    bitSets[KILL] = bsf.makeSet(kill);
		    Temp_to_BitSets.put(t, bitSets);
		}
		for(Iterator temps=DefPts.keySet().iterator(); 
		    temps.hasNext(); ) {
		    Temp t = (Temp)temps.next();
		    Set[] bitSets = (Set[])Temp_to_BitSets.get(t);
		    BitSetFactory bsf = 
			(BitSetFactory)Temp_to_BitSetFactories.get(t);
		    if (bitSets == null) {
			bitSets = new Set[4];
			Temp_to_BitSets.put(t, bitSets);
			bitSets[GEN] = bsf.makeSet(Collections.EMPTY_SET);
			bitSets[KILL] = bsf.makeSet(Collections.EMPTY_SET);
		    }
		    bitSets[IN] = bsf.makeSet(Collections.EMPTY_SET); //in
		    bitSets[OUT] = bsf.makeSet(Collections.EMPTY_SET); //out
		}
	    }
	    m.put(b, Temp_to_BitSets);
	}
	return m;
    }
    private final int IN = 0;
    private final int OUT = 1;
    private final int GEN = 2;
    private final int KILL = 3;
    // uses Worklist algorithm to solve for reaching definitions
    // given a map of BasicBlocks to Maps of Temps to arrays of bit Sets
    private void solve() {
	Map Temp_to_InOut = new HashMap();
	Worklist worklist = new WorkSet(bbf.blockSet());
	while(!worklist.isEmpty()) {
	    BasicBlock b = (BasicBlock)worklist.pull();
	    Map bitSets = (Map)cache.get(b);
	    for(Iterator it=cache.keySet().iterator(); it.hasNext(); ) {
		Temp t = (Temp)it.next();
		Set[] bitSet = (Set[])bitSets.get(t);
		BitSetFactory bsf = 
		    (BitSetFactory)Temp_to_BitSetFactories.get(t);
		Set[] old = new Set[2];
		old[IN] = bsf.makeSet(bitSet[IN]);
		old[OUT] = bsf.makeSet(bitSet[OUT]);
		for(Iterator preds=b.prevSet().iterator(); it.hasNext(); ) {
		    BasicBlock pred = (BasicBlock)it.next();
		    Set[] pBitSet = (Set[])((Map)cache.get(pred)).get(t);
		    bitSet[IN].addAll(pBitSet[OUT]); // union 
		}
		bitSet[OUT] = bsf.makeSet(bitSet[IN]);
		bitSet[OUT].removeAll(bitSet[KILL]);
		bitSet[OUT].addAll(bitSet[GEN]);
		if (old[IN].equals(bitSet[IN]) && 
		    old[OUT].equals(bitSet[OUT]))
		    continue;
		for(Iterator succs=b.nextSet().iterator(); succs.hasNext(); )
		    worklist.push(succs.next());
	    }
	}
    }
    // create a mapping of Temps to a Set of possible definition points
    private Map getDefPts() {
	Map m = new HashMap();
	for(Iterator it=hc.getElementsI(); it.hasNext(); ) {
	    HCodeElement hce = (HCodeElement)it.next();
	    Temp[] tArray = ((UseDef)hce).def();
	    for(int i=0; i < tArray.length; i++) {
		Temp t = tArray[i];
		Set defPts = (Set)m.get(t);
		if (defPts == null) {
		    // have not yet encountered this Temp
		    defPts = new HashSet();
		    defPts.add(hce);
		    m.put(t, defPts);
		} else {
		    // simply add this definition pt to set
		    defPts.add(hce);
		}
	    }
	}
	return m;
    }
    // create a mapping of Temps to BitSetFactories
    // using a mapping of Temps to Sets of definitions points
    private void getBitSets(Map input) {
	for(Iterator it=input.keySet().iterator(); it.hasNext(); ) {
	    Temp t = (Temp)it.next();
	    BitSetFactory bsf = new BitSetFactory((Set)input.get(t));
	    Temp_to_BitSetFactories.put(t, bsf);
	}
    }
}





