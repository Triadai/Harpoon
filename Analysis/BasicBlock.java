// BasicBlock.java, created Wed Mar 10  9:00:53 1999 by jwhaley
// Copyright (C) 1998 John Whaley
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis;

import harpoon.Util.Util;
import harpoon.Util.IteratorEnumerator;
import harpoon.Util.WorkSet;
import harpoon.Util.Worklist;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HCodeEdge;
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

/**
   BasicBlock collects a serial list of operations.  It is designed to
   abstract away specific operation and allow the compiler to focus on
   control flow at a higher level.  (It also allows for some analysis
   within the block to operate more efficiently by taking advantage of
   the fact that the elements of a BasicBlock have a total ordering of
   execution). 

   <BR> <B>NOTE:</B> right now <code>BasicBlock</code> only guarantees
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
 * @version $Id: BasicBlock.java,v 1.1.2.16 2000-02-01 00:56:12 pnkfelix Exp $
*/
public class BasicBlock {
    
    static final boolean DEBUG = false;

    // tracks the current id number to assign to the next generated
    // basic block
    private static int BBnum = 0;
    
    private HCodeElement first;
    private HCodeElement last;
    private CFGrapher grapher;
    private Set pred_bb;
    private Set succ_bb;

    // unique id number for this basic block
    private int num;

    // number of statements in this block
    private int size;
    
    private BasicBlock root;
    private Set leaves;
    
    // each block contains a reference to a Map shared between all of
    // the blocks that it is connected to.  It provides a mapping from
    // every instr that is contained in these blocks to the BasicBlock
    // containing that instr. 
    private Map hceToBB; 

    /** Returns a <code>Map</code> from every
	<code>HCodeElement</code> that 	is contained in this
	collection of basic blocks to the <code>BasicBlock</code>
	containing that <code>HCodeElement</code>. 
    */
    public Map getHceToBB() {
	return Collections.unmodifiableMap(hceToBB);
    }

    /** BasicBlock Iterator generator.
	<BR> <B>effects:</B> returns an <code>Iterator</code> over all
	of the <code>BasicBlock</code>s linked to and from
	<code>block</code>.  This <code>Iterator</code> will return
	each <code>BasicBlock</code> no more than once.
    */
    public static Iterator basicBlockIterator(BasicBlock block) { 
	ArrayList lst = new ArrayList();
	WorkSet todo = new WorkSet();
	lst.add(block);
	todo.push(block);
	while( !todo.isEmpty() ) {
	    BasicBlock doing = (BasicBlock) todo.pull(); 
	    Enumeration enum = doing.next(); 
	    while(enum.hasMoreElements()) { 
		BasicBlock b = (BasicBlock) enum.nextElement(); 
		if (!lst.contains(b)) {
		    lst.add(b);
		    todo.push(b);
		}
	    } 
	    enum = doing.prev();  
	    while(enum.hasMoreElements()) {
		BasicBlock b = (BasicBlock) enum.nextElement(); 
		if (!lst.contains(b)) {
		    lst.add(b);
		    todo.push(b); 
		}
	    } 
	}
	return lst.iterator();
    }

    public Iterator blocksIterator() {
	return basicBlockIterator(this);
    }

    
    /** BasicBlock generator.
	<BR> <B>requires:</B> 
	      <code>head</code> is an appropriate entry point for a
	      basic block (I'm working on eliminating this
	      requirement, but for now its safer to keep it)

	<BR> <B>effects:</B>  Creates a set of
	     <code>BasicBlock</code>s corresponding to the blocks
	     implicitly contained in <code>head</code> and the
	     <code>HCodeElement</code> objects that <code>head</code>
	     points to, and returns the <code>BasicBlock</code> that
	     <code>head</code> is an instruction in.  The
	     <code>BasicBlock</code> returned is considered to be the
	     root (entry-point) of the set of <code>BasicBlock</code>s
	     created.
    */
    public static BasicBlock computeBasicBlocks(HCodeElement head,
						final CFGrapher gr) {
	// maps from every hce 'h' -> BasicBlock 'b' such that 'b'
	// contains 'h' 
	HashMap hceToBB = new HashMap();

	// maps HCodeElement 'e' -> BasicBlock 'b' starting with 'e'
	Hashtable h = new Hashtable(); 
	// stores BasicBlocks to be processed
	Worklist w = new WorkSet();

	while (gr.pred(head).length == 1) {
	    head = gr.pred(head)[0].from();
	}
	
	BasicBlock first = new BasicBlock(head, gr);
	first.hceToBB = hceToBB;
	h.put(head, first);
	hceToBB.put(head, first);
	w.push(first);
	
	first.root = first;
	first.leaves = new HashSet();

	while(!w.isEmpty()) {
	    BasicBlock current = (BasicBlock) w.pull();
	    
	    // 'last' is our guess on which elem will be the last;
	    // thus we start with the most conservative guess
	    HCodeElement last = current.getFirst();
	    boolean foundEnd = false;
	    while(!foundEnd) {
		int n = gr.succC(last).size();
		if (n == 0) {
		    if(DEBUG) System.out.println("found end:   "+last);

		    foundEnd = true;
		    first.leaves.add(current); 

		} else if (n > 1) { // control flow split
		    if(DEBUG) System.out.println("found split: "+last);

		    for (int i=0; i<n; i++) {
			HCodeElement e_n = gr.succ(last)[i].to();
			BasicBlock bb = (BasicBlock) h.get(e_n);
			if (bb == null) {
			    h.put(e_n, bb=new BasicBlock(e_n, gr));
			    bb.hceToBB = hceToBB;
			    hceToBB.put(e_n, bb);
			    bb.root = first; bb.leaves = first.leaves;
			    w.push(bb);
			}
			addEdge(current, bb);
		    }
		    foundEnd = true;
		    
		} else { // one successor
		    Util.assert(n == 1, "must have one successor");
		    HCodeElement next = gr.succ(last)[0].to();
		    int m = gr.predC(next).size();
		    if (m > 1) { // control flow join
			if(DEBUG) System.out.println("found join:  "+next);

			BasicBlock bb = (BasicBlock) h.get(next);
			if (bb == null) {
			    bb = new BasicBlock(next, gr);
			    bb.hceToBB = hceToBB;
			    bb.root = first; bb.leaves = first.leaves;
			    h.put(next, bb);
			    hceToBB.put(next, bb);
			    w.push(bb);
			}
			addEdge(current, bb);
			foundEnd = true;
			
		    } else { // no join; update our guess
			if(DEBUG) System.out.println("found line:  "+
						     last+", "+ next);
			
			current.size++;
			hceToBB.put(next, current);
			last = next;
		    }
		}
	    }
	    current.last = last;
	}

	if (false) { // check that all reachables are included in our BasicBlocks somewhere.
	    HashSet checked = new HashSet();

	    ArrayList todo = new ArrayList();
	    todo.add(head);

	    while(!todo.isEmpty()) {
		HCodeElement hce =(HCodeElement) todo.remove(0);
 		BasicBlock bb = (BasicBlock) hceToBB.get(hce);
		Util.assert(bb != null, "hce "+hce+" should map to some BB");
		boolean missing = true;
		Iterator elems = bb.statements().iterator(); 
		while(missing && elems.hasNext()) {
		    HCodeElement o = (HCodeElement) elems.next();
		    if (hce.equals(o)) missing = false;
		}
		Util.assert(!missing, 
			    "hce "+hce+" should be somewhere in "+
			    "BB "+bb);
		checked.add(hce);
		Iterator predEdges = gr.predC(hce).iterator();
		while(predEdges.hasNext()) {
		    HCodeEdge edge = (HCodeEdge) predEdges.next();
		    if (!checked.contains(edge.from()) &&
			!todo.contains(edge.from())) {
			todo.add(edge.from());
		    } 
		}
		Iterator succEdges = gr.succC(hce).iterator();
		while(succEdges.hasNext()) {
		    HCodeEdge edge = (HCodeEdge) succEdges.next();
		    if (!checked.contains(edge.to()) &&
			!todo.contains(edge.to())) {
			todo.add(edge.to());
		    } 
		}
	    }
	}

	return (BasicBlock) h.get(head);
    }

    public HCodeElement getFirst() { return first; }
    public HCodeElement getLast() { return last; }
    
    private void addPredecessor(BasicBlock bb) { pred_bb.add(bb); }
    private void addSuccessor(BasicBlock bb) { succ_bb.add(bb); }
    
    public int prevLength() { return pred_bb.size(); }
    public int nextLength() { return succ_bb.size(); }
    public Enumeration prev() { return new IteratorEnumerator(pred_bb.iterator()); }
    public Enumeration next() { return new IteratorEnumerator(succ_bb.iterator()); }

    /** Returns all the predecessors of <code>this</code> basic block. */
    public BasicBlock[] getPrev() {
	return (BasicBlock[]) pred_bb.toArray(new BasicBlock[pred_bb.size()]);
    }

    /** Returns all the successors of <code>this</code> basic block. */
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
	Util.assert(size > 0, "BasicBlock class breaks on empty bbs");
	return new java.util.AbstractSequentialList() {
	    public int size() { return size; }
	    public ListIterator listIterator(int index) {
		// note that index *can* equal the size of the list,
		// in which case we start the iterator past the last
		// element of the list. 

		// check argument
		if (index < 0 || index > size) {
		    throw new IndexOutOfBoundsException
			(index +"< 0 || "+index+" > "+size);
		}
		
		// iterate to correct starting point
		HCodeElement curr = first;
		int i;

		// slight rep inconsistency in upper bound; we'll keep
		// next pointing at curr, even though that's the
		// prev-elem, not the next one.  See implementation
		// below for details 
		int bound = Math.min(index, size-1);
		for(i=0; i < bound; i++) {
		    curr = grapher.succ(curr)[0].to();
		}
		
		// new final vars to be passed to ListIterator
		final HCodeElement fcurr = curr;
		final int fi = index;

		if (false) System.out.println
			       (" generating listIterator("+index+")"+
				" next: "+fcurr+
				" ind: "+fi);

		return new harpoon.Util.UnmodifiableListIterator() {
		    HCodeElement next = fcurr; //elem for next() to return
		    int ind = fi; //where currently pointing?

		    public boolean hasNext() {
			return ind!=size;
		    }
		    public Object next() {
			if (!hasNext()) {
			    throw new NoSuchElementException();
			}
			ind++;
			Object ret = next;
			Util.assert(ind <= size, 
				    "ind > size:"+ind+", "+size);
			if (ind != size) {
			    HCodeEdge[] succs = grapher.succ(next);
			    Util.assert(succs.length == 1,
					next+" has wrong succs:" + 
					java.util.Arrays.asList(succs)+
					" (ind: "+ind+")");
			    next = succs[0].to(); 

			} else { 
			    // keep 'next' the same, since previous()
			    // needs to be able to return it
			}
			return ret;
		    }
		    
		    
		    public boolean hasPrevious() {
			if (ind == size) {
			    return true;
			} else {
			    return grapher.predC(next).size() == 1;
			}
		    }
		    public Object previous() {
			if (!hasPrevious()) {
			    throw new NoSuchElementException();
			}
			if (ind != size) {
			    next = grapher.pred(next)[0].from();
			}
			ind--;
			return next;
		    } 
		    public int nextIndex() {
			return ind;
		    }
		};
	    }
	};
    }

    /** Accept a visitor. */
    public void accept(BasicBlockVisitor v) { v.visit(this); }
    
    protected BasicBlock(HCodeElement f, CFGrapher gr) {
	first = f; last = null; 
	pred_bb = new HashSet(); succ_bb = new HashSet();
	grapher = gr;
	num = BBnum++;
	size = 1;
    }

    /** Returns the root <code>BasicBlock</code>.
	<BR> <B>effects:</B> returns the <code>BasicBlock</code> that
	     is at the start of the set of <code>HCodeElement</code>s
	     being analyzed.
    */
    public BasicBlock getRoot() {
	return root;
    }

    /** Returns the leaf <code>BasicBlock</code>s.
	<BR> <B>effects:</B> returns a <code>Set</code> of
	     <code>BasicBlock</code>s that are at the ends of the
	     <code>HasEdge</code>s being analyzed.  
    */
    public Set getLeaves() {
	return Collections.unmodifiableSet(leaves);
    }

    private static void addEdge(BasicBlock from, BasicBlock to) {
	from.addSuccessor(to);
	to.addPredecessor(from);
    }
    
    public String toString() {
	return "BB"+num;
    }

    public String dumpElems() {
	StringBuffer s = new StringBuffer();
	Iterator iter = statements().listIterator();
	while(iter.hasNext()) {	    
	    s.append(iter.next() + "\n");
	}
	return s.toString();
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
