// BasicBlock.java, created Wed Mar 10  9:00:53 1999 by jwhaley
// Copyright (C) 1998 John Whaley
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis;

import harpoon.Util.Util;
import harpoon.Util.IteratorEnumerator;
import harpoon.Util.WorkSet;
import harpoon.Util.Collections.LinearSet;
import harpoon.Util.Worklist;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HCodeEdge;
import harpoon.ClassFile.HCode;
import harpoon.IR.Properties.CFGrapher;

import harpoon.Analysis.DataFlow.ReversePostOrderEnumerator;

import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Collection;

/**
   BasicBlock collects a sequence of operations.  It is designed to
   abstract away specific operation and allow the compiler to focus on
   control flow at a higher level.  (It also allows for some analysis
   within the block to operate more efficiently by taking advantage of
   the fact that the elements of a BasicBlock have a total ordering of
   execution). 

   <P> Most BasicBlocks are a part of a larger piece of code, and thus
   a collection of BasicBlocks form a Control Flow Graph (where the
   nodes of the graph are the blocks and the directed edges of the
   graph indicate when one block is succeeded by another block).
   
   <P> Make sure to look at <code>BasicBlock.Factory</code>, since it
   acts as the central core for creating and keeping track of the
   <code>BasicBlock</code>s for a given <code>HCode</code>.

   <P> <B>NOTE:</B> right now <code>BasicBlock</code> only guarantees
   that it preserves the Maximal Basic Block property (where the first
   element is the entry point and the last element is the exit point)
   if the graph of operations is not modified while the basic block is
   in use.  However, other pieces of code WILL modify the Beginning
   and End of a basic block (for example, the register allocator will
   add LOADs to the beginning and STOREs to the end).  Perhaps we can
   allow for some modification of the Control-Flow-Graph; check with
   group. 

 *
 * @author  John Whaley
 * @author  Felix Klock <pnkfelix@mit.edu> 
 * @version $Id: BasicBlock.java,v 1.1.2.31 2000-06-26 22:35:55 pnkfelix Exp $ */
public class BasicBlock {
    
    static final boolean DEBUG = false;
    static final boolean TIME = false;
    
    static final boolean CHECK_INSTRS = false;

    private HCodeElement first;
    private HCodeElement last;

    // BasicBlocks preceding and succeeding this block (we store the
    // CFG implicityly in the basic block objects; if necessary this
    // information can be migrated to BasicBlock.Factory and
    // maintained there)
    private Set pred_bb;
    private Set succ_bb;

    // unique id number for this basic block; used only for BasicBlock.toString()
    private int num;

    // number of statements in this block
    private int size;
    
    // factory that generated this block
    private Factory factory; 

    /** Returns the first <code>HCodeElement</code> in the sequence. 
	@deprecated Use the standard List view provided by statements() instead
	@see BasicBlock#statements
     */
    public HCodeElement getFirst() { return first; }

    /** Returns the last <code>HCodeElement</code> in the sequence. 
	@deprecated Use the standard List view provided by statements() instead
	@see BasicBlock#statements
     */
    public HCodeElement getLast() { return last; }
    
    /** Adds <code>bb</code> to the set of predecessor basic blocks
	for <code>this</code>.  Meant to be used during construction.
	FSK: probably should take this out; it adds little to the
	class 
    */
    private void addPredecessor(BasicBlock bb) { pred_bb.add(bb); }

    /** Adds <code>bb</code> to the set of successor basic blocks for
	<code>this</code>.  Meant to be used during construction.
	FSK: probably should take this out; it adds little to the
	class 
    */
    private void addSuccessor(BasicBlock bb) { succ_bb.add(bb); }
    
    /** Returns the number of basic blocks in the predecessor set for
	<code>this</code>. 
	@deprecated Use prevSet() instead
	@see BasicBlock#prevSet
    */
    public int prevLength() { return pred_bb.size(); }

    /** Returns the number of basic blocks in the successor set for
	<code>this</code>. 
	@deprecated Use nextSet() instead
	@see BasicBlock#nextSet
    */
    public int nextLength() { return succ_bb.size(); }

    /** Returns an Enumeration that iterates over the predecessors for
	<code>this</code>. 
	@deprecated Use prevSet() instead
	@see BasicBlock#prevSet
    */
    public Enumeration prev() { return new IteratorEnumerator(pred_bb.iterator()); }

    /** Returns an Enumeration that iterates over the successors for
	<code>this</code>. 
	@deprecated Use nextSet() instead
	@see BasicBlock#prevSet
    */
    public Enumeration next() { return new IteratorEnumerator(succ_bb.iterator()); }

    /** Returns all the predecessors of <code>this</code> basic
	block. 
	@deprecated Use prevSet() instead
	@see BasicBlock#prevSet
    */
    public BasicBlock[] getPrev() {
	return (BasicBlock[]) pred_bb.toArray(new BasicBlock[pred_bb.size()]);
    }

    /** Returns all the successors of <code>this</code> basic block. 
	@deprecated Use nextSet() instead
	@see BasicBlock#nextSet
    */
    public BasicBlock[] getNext() {
	return (BasicBlock[]) succ_bb.toArray(new BasicBlock[succ_bb.size()]);
    }

    /** Returns all the predecessors of <code>this</code>. 
	<BR> <B>effects:</B> returns a <code>Set</code> of
	     <code>BasicBlock</code>s which precede
	     <code>this</code>. 
     */
    public Set prevSet() { 
	return Collections.unmodifiableSet(pred_bb); 
    }
    
    /** Returns all the successors of <code>this</code>. 
	<BR> <B>effects:</B> returns a <code>Set</code> of
	     <code>BasicBlock</code>s which succeed
	     <code>this</code>. 
     */
    public Set nextSet() {
	return Collections.unmodifiableSet(succ_bb);
    }

    /** Returns an unmodifiable <code>List</code> for the
	<code>HCodeElement</code>s within <code>this</code>.  

	<BR> <B>effects:</B> Generates a new <code>List</code> of
	<code>HCodeElement</code>s ordered according to the order
	mandated by the <code>CFGrapher</code> used in the call to
	<code>computeBasicBlocks</code> that generated
	<code>this</code>. 
    */
    public List statements() {

	// FSK: this is dumb; why not just return an empty list in
	// this case?  I suspect this is an attempt to fail-fast, but
	// still... 
	Util.assert(size > 0, "BasicBlock class breaks on empty BBs");

	return new java.util.AbstractSequentialList() {
	    public int size() { return size; }
	    public ListIterator listIterator(int index) {
		// note that index *can* equal the size of the list,
		// in which case we start the iterator past the last
		// element of the list. 

		// check argument
		if (index < 0) {
		    throw new IndexOutOfBoundsException(index +"< 0"); 
		} else if (index > size) {
		    throw new IndexOutOfBoundsException(index+" > "+size); 
		}
		
		// iterate to correct starting point
		HCodeElement curr;

		if (index < size) {
		    curr = first;
		    int bound = Math.min(index, size-1);
		    for(int i=0; i < bound; i++) {
			curr = factory.grapher.succ(curr)[0].to();
		    }
		} else {
		    curr = last;
		}

		// new final vars to be passed to ListIterator
		final HCodeElement fcurr = curr;
		final int fi = index;

		if (false) System.out.println
			       (" generating listIterator("+index+")"+
				" next: "+fcurr+
				" ind: "+fi);

		return new harpoon.Util.UnmodifiableListIterator() {
		    //elem for next() to return
		    HCodeElement next = fcurr; 
		    
		    // where currently pointing?  
		    // Invariant: 0 <= ind /\ ind <= size 
		    int ind = fi; 

		    // checks rep of `this' (for debugging)
		    private void repOK() {
			repOK("");
		    }

		    // checks rep of `this' (for debugging)
		    private void repOK(String s) {
			Util.assert(0 <= ind, s+" (0 <= ind), ind:"+ind);
			Util.assert(ind <= size, s+" (ind <= size), ind:"+ind+", size:"+size);
			Util.assert( (ind==0)?next==first:true,
				     s+" (ind==0 => next==first), next:"+next+", first:"+first);

			Util.assert( (ind==(size-1))?next==last:true,
				     s+" (ind==(size-1) => next==last), next:"+next+", last:"+last);

			Util.assert( (ind==size)?next==last:true,
				     s+" (ind==size => next==last), next:"+next+", last:"+last);
		    }

		    public boolean hasNext() {
			if (DEBUG) repOK();
			return ind != size;
		    }
		    public Object next() {
			if (DEBUG) repOK("beginning");			
			if (ind == size) {
			    throw new NoSuchElementException();
			}
			ind++;
			Object ret = next;
			if (ind != size) {
			    Collection succs = factory.grapher.succC(next);
			    Util.assert(succs.size() == 1,
					next+" has wrong succs:" + 
					succs+" (ind:"+ind+", size:"+size+")");
			    next = ((HCodeEdge)succs.iterator().next()).to(); 

			} else { 
			    // keep 'next' the same, since previous()
			    // needs to be able to return it
			}

			if (DEBUG) repOK("end");
			return ret;
		    }
		    
		    
		    public boolean hasPrevious() {
			if (DEBUG) repOK();
			return ind > 0;

		    }
		    public Object previous() {
			if (DEBUG) repOK();

			if (ind <= 0) {
			    throw new NoSuchElementException();
			}

			// special case: if ind == size, then we just
			// return <next>
			if (ind != size) {
			    next = factory.grapher.pred(next)[0].from();
			}
			ind--;

			if (DEBUG) repOK();
			return next;
		    } 
		    public int nextIndex() {
			if (DEBUG) repOK();
			return ind;
		    }
		};
	    }
	};
    }

    /** Accept a visitor. 
	FSK: is this really useful?  John put this in with the thought
	that we'd have many different types of BasicBlocks, but I'm
	not sure about that actually being a useful set of subtypes
     */
    public void accept(BasicBlockVisitor v) { v.visit(this); }
    
    /** Constructs a new BasicBlock with <code>h</code> as its first
	element.  Meant to be used only during construction.
    */
    protected BasicBlock(HCodeElement h, Factory f) {
	Util.assert(h!=null);
	first = h; 
	last = null; // note that this MUST be updated by 'f'
	pred_bb = new HashSet(); succ_bb = new HashSet();
	size = 1;
	this.factory = f;
	num = factory.BBnum++;
    }

    /** Constructs an edge from <code>from</code> to
	</code>to</code>. 
    */
    private static void addEdge(BasicBlock from, BasicBlock to) {
	from.addSuccessor(to);
	to.addPredecessor(from);
    }
    
    public String toString() {
	return "BB"+num;
    }

    /** Returns a String composed of the statements comprising
	<code>this</code>. 
    */
    public String dumpElems() {
	StringBuffer s = new StringBuffer();
	Iterator iter = statements().listIterator();
	while(iter.hasNext()) {	    
	    s.append(iter.next() + "\n");
	}
	return s.toString();
    }
    
    /** Factory structure for generating BasicBlock views of
	an <code>HCode</code>.  	
    */
    public static class Factory { 
	// the underlying HCode
	private final HCode hcode;

	private final Map hceToBB;

	private final CFGrapher grapher;
	private final BasicBlock root;
	private final Set leaves;
	private final Set blocks;


	// tracks the current id number to assign to the next
	// generated basic block
	private int BBnum = 0;

	/** Returns the root <code>BasicBlock</code>.
	    <BR> <B>effects:</B> returns the <code>BasicBlock</code>
	         that is at the start of the set of
		 <code>HCodeElement</code>s being analyzed.
	*/
	public BasicBlock getRoot() {
	    return root;
	}

	/** Returns the leaf <code>BasicBlock</code>s.
	    <BR> <B>effects:</B> returns a <code>Set</code> of
	         <code>BasicBlock</code>s that are at the ends of the
		 <code>HCodeElement</code>s being analyzed.
	*/
	public Set getLeaves() {
	    return leaves;
	}

	/** Returns the <code>HCode</code> that <code>this</code> factory
	    produces basic blocks of. */
	public HCode getHCode(){
	    return hcode;
	}

	/** Returns the <code>BasicBlock</code>s constructed by
	    <code>this</code>.
	*/
	public Set blockSet() {
	    return blocks;
	}
	
	/** Generates an <code>Iterator</code> that traverses over all
	    of the blocks generated by this <code>BasicBlock.Factory</code>.
	*/
	public Iterator blocksIterator() {
	    return blockSet().iterator();
	}

	/** Returns the <code>BasicBlock</code> containing
	    <code>hce</code>. 
	*/
	public BasicBlock getBlock(HCodeElement hce) {
	    return (BasicBlock) hceToBB.get(hce);
	}

	/** Constructs a <code>BasicBlock.Factory</code> and generates
	    <code>BasicBlock</code>s for a given <code>HCode</code>.
	    <BR> <B>requires:</B> 
	         <code>grapher.getFirstElement(hcode)</code>
	         is an appropriate entry point for a 
		 basic block.
	    <BR> <B>effects:</B>  Creates a set of
	         <code>BasicBlock</code>s corresponding to the blocks
		 implicitly contained in
		 <code>grapher.getFirstElement(hcode)</code> and the
		 <code>HCodeElement</code> objects that this
		 points to, and returns the
		 <code>BasicBlock</code> that
		 <code>grapher.getFirstElement(hcode)</code> is an
		 instruction in.  The <code>BasicBlock</code> returned
		 is considered to be the root (entry-point) of the set
		 of <code>BasicBlock</code>s created.   
	*/
	public Factory(HCode hcode, final CFGrapher grapher) {
	    if (TIME) System.out.print("bldBB");

	    // maps HCodeElement 'e' -> BasicBlock 'b' starting with 'e'
	    HashMap h = new HashMap(); 
	    // stores BasicBlocks to be processed
	    Worklist w = new WorkSet();

	    HCodeElement head = grapher.getFirstElement(hcode);
	    this.grapher = grapher;
	    this.hcode   = hcode;

	    // modifable util classes for construction use only
	    HashSet myLeaves = new HashSet();
	    HashMap myHceToBB = new HashMap();

	    BasicBlock first = new BasicBlock(head, this);
	    h.put(head, first);
	    myHceToBB.put(head, first);
	    w.push(first);
	    
	    root = first;



	    while(!w.isEmpty()) {
		BasicBlock current = (BasicBlock) w.pull();
		
		// 'last' is our guess on which elem will be the last;
		// thus we start with the most conservative guess
		HCodeElement last = current.getFirst();
		boolean foundEnd = false;
		while(!foundEnd) {
		    int n = grapher.succC(last).size();
		    if (n == 0) {
			if(DEBUG) System.out.println("found end:   "+last);
			
			foundEnd = true;
			myLeaves.add(current); 
			
		    } else if (n > 1) { // control flow split
			if(DEBUG) System.out.println("found split: "+last);
			
			for (int i=0; i<n; i++) {
			    HCodeElement e_n = grapher.succ(last)[i].to();
			    BasicBlock bb = (BasicBlock) h.get(e_n);
			    if (bb == null) {
				h.put(e_n, bb=new BasicBlock(e_n, this));
				myHceToBB.put(e_n, bb);
				w.push(bb);
			    }
			    BasicBlock.addEdge(current, bb);
			}
			foundEnd = true;
			
		    } else { // one successor
			Util.assert(n == 1, "must have one successor");
			HCodeElement next = grapher.succ(last)[0].to();
			int m = grapher.predC(next).size();
			if (m > 1) { // control flow join
			    if(DEBUG) System.out.println("found join:  "+next);
			    
			    BasicBlock bb = (BasicBlock) h.get(next);
			    if (bb == null) {
				bb = new BasicBlock(next, this);
				h.put(next, bb);
				myHceToBB.put(next, bb);
				w.push(bb);
			    }
			    BasicBlock.addEdge(current, bb);
			    foundEnd = true;
			    
			} else { // no join; update our guess
			    if(DEBUG) System.out.println("found line:  "+
							 last+", "+ next);
			    
			    current.size++;
			    myHceToBB.put(next, current);
			    last = next;
			}
		    }
		}

		current.last = last;

		final HCodeElement flast = last;
		final BasicBlock fcurr = current;
		Util.assert( grapher.succC(last).size() != 1 ||
			     grapher.predC(grapher.succ(last)[0].
				      to()).size() > 1,
			     new Object() { 
				 public String toString() {
				     return 
				     "last elem: "+flast+" of "+ 
				     fcurr+" breaks succC "+
				     "invariant: "+grapher.succC(flast)+
				     " BB: " + fcurr.dumpElems();
				 }
			     });

	    }

	    // efficiency hacks: make various immutable Collections
	    // array-backed sets, and make them unmodifiable at
	    // construction time rather than at accessor time.
	    leaves = Collections.unmodifiableSet(new LinearSet(myLeaves));
	    hceToBB = Collections.unmodifiableMap(myHceToBB);
	    blocks = Collections.unmodifiableSet(new LinearSet
						 (new HashSet(hceToBB.values())));
	    Iterator bbIter = blocks.iterator();
	    while (bbIter.hasNext()) {
		BasicBlock bb = (BasicBlock) bbIter.next();
		bb.pred_bb = new LinearSet(bb.pred_bb);
		bb.succ_bb = new LinearSet(bb.succ_bb);

		// FSK: debug checkBlock(bb);
	    }

	    if (CHECK_INSTRS) {
		// check that all instrs map to SOME block
		Iterator hceIter = hcode.getElementsI();
		while(hceIter.hasNext()) {
		    HCodeElement hce = (HCodeElement) hceIter.next();
		    if (!(hce instanceof harpoon.IR.Assem.InstrLABEL) &&
			!(hce instanceof harpoon.IR.Assem.InstrDIRECTIVE)&&
			!(hce instanceof harpoon.IR.Assem.InstrJUMP))
			Util.assert(getBlock(hce) != null, "no BB for "+hce);
		}
	    }
		

	    if (TIME) System.out.print("#");	    
	}

	private void checkBlock(BasicBlock block) {
	    List blockL = block.statements();
	    int sz = blockL.size();
	    Iterator iter = blockL.iterator();
	    HCodeElement curr = null;
	    while(iter.hasNext()) {
		HCodeElement h = (HCodeElement) iter.next();
		if (curr == null) {
		    Util.assert(h == block.first);

		    curr = h;
		} else {
		    Util.assert(grapher.succC(curr).size() == 1);
		    Util.assert(grapher.succ(curr)[0].to() == h,
				"curr:"+curr+" succ:"+grapher.succ(curr)[0].to()+
				" h:"+h);

		    curr = h;
		}
		sz--;
	    }
	    Util.assert(curr == block.last);
	    Util.assert(sz == 0);
	}

	public static void dumpCFG(BasicBlock start) {
	    Enumeration e = new ReversePostOrderEnumerator(start);
	    while (e.hasMoreElements()) {
		BasicBlock bb = (BasicBlock)e.nextElement();
		System.out.println("Basic block "+bb + " size:"+ bb.size);
		System.out.println("BasicBlock in : "+bb.pred_bb);
		System.out.println("BasicBlock out: "+bb.succ_bb);
		System.out.println();
	    }
	}
    }
}
