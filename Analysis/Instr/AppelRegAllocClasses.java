// AppelRegAllocClasses.java, created Tue Jun  5  0:02:27 2001 by pnkfelix
// Copyright (C) 2001 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.Instr;

import harpoon.IR.Assem.Instr;

import harpoon.Temp.Temp;

import harpoon.Util.Util;
import harpoon.Util.Default;
import harpoon.Util.UnmodifiableIterator;
import harpoon.Util.Collections.LinearSet;

import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/** Collects various data structures used by AppelRegAlloc. 
 *  @author  Felix S. Klock II <pnkfelix@mit.edu>
 *  @version $Id: AppelRegAllocClasses.java,v 1.1.2.7 2001-06-21 04:53:46 pnkfelix Exp $
 */
abstract class AppelRegAllocClasses extends RegAlloc {
    public static final boolean CHECK_INV = false;

    public AppelRegAllocClasses(harpoon.Backend.Generic.Code code) { 
	super(code); 
    }
    
    private String lastStateString;
    /** Saves the current state of <code>this</code> for later
	retrieval using <code>resetState()</code>.  

	Note that <code>this</code> only carries <code>Node</code> and
	<class>Move</code> state; additional state added by subclasses
	will not be checkpointed unless this method and reset state
	are overridden.

    */
    protected void checkpointState() { 
	if (CHECK_INV) 
	    lastStateString = stateString();
	saveNodePairSet();
	saveNodeSets();
	if (CHECK_INV)
	    Util.assert( lastStateString.equals(stateString()), "\n\n"
			 +"last : "+lastStateString+"\n"
			 +"curr : "+stateString()+"\n"
			 );
    }
    /** Restores the state of <code>this</code> to the state it was in
	on the last call to <code>checkpointState()</code>.  
    */
    protected void resetState() {
	restoreNodePairSet();
	restoreNodeSets();
	resetMoveSets();
	if (CHECK_INV)
	    Util.assert( lastStateString.equals(stateString()), "\n\n"
			 +"last : "+lastStateString+"\n"
			 +"curr : "+stateString()+"\n"
			 );
    }

    private String stateString() {
	// FSK: should probably sort lists on index number to ensure
	// determinism on sets.
	String igraphString = interferenceGraphString();

	return "\n"+
	    precolored+"\n"+
	    initial+"\n"+
	    simplify_worklist+"\n"+
	    freeze_worklist+"\n"+
	    spill_worklist+"\n"+
	    spilled_nodes+"\n"+
	    coalesced_nodes+"\n"+
	    colored_nodes+"\n"+
	    select_stack+"\n"+
	    "\n\n"+
	    coalesced_moves+"\n"+
	    constrained_moves+"\n"+
	    frozen_moves+"\n"+
	    worklist_moves+"\n"+
	    active_moves+"\n"+
	    "\n\n"+
	    igraphString
	    ;
    }

    String interferenceGraphString() {
	StringBuffer sb = new StringBuffer();
	sb.append("** BEGIN GRAPH **\n");
	for(Iterator nodes = allNodes.iterator(); nodes.hasNext(); ){
	    Node n = (Node) nodes.next();
	    sb.append(n);
	    sb.append(n.id);
	    sb.append(", neighbors:[");
	    for(NodeIter nbors = n.neighbors.iter(); nbors.hasNext();){
		sb.append( nbors.next().id );
	    }
	    sb.append("]");
	    sb.append("\n");
	}
	sb.append("** END GRAPH **\n");
	
	sb.append("AdjSet: ");
	java.util.TreeSet sortedPairs = 
	    new java.util.TreeSet( new java.util.Comparator() {
		    public int compare(Object o1, Object o2) {
			List nl_0 = (List) o1;
			List nl_1 = (List) o2;
			Node n00 = (Node) nl_0.get(0);
			Node n01 = (Node) nl_0.get(1);
			Node n10 = (Node) nl_1.get(0);
			Node n11 = (Node) nl_1.get(1);
			
			if (n00.id == n10.id) {
			    return n01.id - n11.id;
			} else {
			    return n00.id - n10.id;
			}
		    }
		});
	sortedPairs.addAll( adjSet.pairs );
	sb.append( sortedPairs );
	
	return sb.toString();
    }

    // Set of <Node, Node> pairs
    NodePairSet adjSet;

    NodeSet precolored;
    NodeSet initial;
    NodeSet simplify_worklist;
    NodeSet freeze_worklist;
    NodeSet spill_worklist;
    NodeSet spilled_nodes;
    NodeSet coalesced_nodes;
    NodeSet colored_nodes;
    NodeSet select_stack;


    static class Web { 
	Temp temp; 
	Collection defs, uses;
	Web(Temp temp, Set defs, Set uses) { 
	    this.temp = temp; 
	    this.defs = new LinearSet(defs); 
	    this.uses = new LinearSet(uses);
	}
	public String toString() {
	    return "Web<"+temp+
		", defs:"+defs+
		", uses:"+uses+">";
	}
    }


    // these sets collectively partition the space of Nodes
    void initializeNodeSets() {
	precolored        = new NodeSet("precolored");
	initial           = new NodeSet("initial");
	simplify_worklist = new NodeSet("simplify_worklist");
	freeze_worklist   = new NodeSet("freeze_worklist");
	spill_worklist    = new NodeSet("spill_worklist");
	spilled_nodes     = new NodeSet("spilled_nodes");
	coalesced_nodes   = new NodeSet("coalesced_nodes");
	colored_nodes     = new NodeSet("colored_nodes");
	select_stack      = new NodeSet("select_stack");
    }

    class BackupNodeSetInfo {
	class BackupNodeInfo {
	    // keeps id/web saved
	    final Node origNode;
	    // don't need to save s_prev/s_next/s_rep
	    final int origDegree;
	    final NodeList origNeighbors; 
	    final Node origAlias;
	    final MoveList origMoves; // List<Move>
	    final int origColor;

	    BackupNodeInfo( Node save ){
		origNode = save;
		origDegree = save.degree;
		origAlias = save.alias;
		origColor = save.color;
		origMoves = new MoveList(save.moves.toList());
		origNeighbors = new NodeList( save.neighbors.toList() );
	    }

	    void restore() {
		origNode.degree = origDegree;
		origNode.alias = origAlias;
		origNode.color = origColor;
		origNode.moves = new MoveList( origMoves.toList() );
		origNode.neighbors = new NodeList( origNeighbors.toList() );
	    }
	}
	
	final NodeSet origSet;
	final ArrayList savedState; // List<BackupNodeInfo>

	BackupNodeSetInfo(NodeSet save) {
	    this.origSet = save;
	    savedState = new ArrayList(origSet.size);
	    for(NodeIter nI=save.iter(); nI.hasNext(); ){
		savedState.add( new BackupNodeInfo( nI.next() ));
	    }
	}
	
	private void clearOrig() { origSet.clear(); }
	private void restore() {
	    for(int i=savedState.size()-1; i>=0; i--){
		BackupNodeInfo bn = (BackupNodeInfo) savedState.get(i);
		bn.restore();
		origSet.add( bn.origNode );
	    }
	}
    }

    BackupNodeSetInfo
	initialI, // wasn't originally in code... was there a reason?

	simplify_worklistI, freeze_worklistI,
	spill_worklistI, spilled_nodesI,
	coalesced_nodesI, colored_nodesI,
	select_stackI;

    private void saveNodeSets() {
	initialI = new BackupNodeSetInfo(  initial );
	simplify_worklistI = new BackupNodeSetInfo(  simplify_worklist );
	freeze_worklistI   = new BackupNodeSetInfo(  freeze_worklist   );
	spill_worklistI	   = new BackupNodeSetInfo(  spill_worklist    );
	spilled_nodesI	   = new BackupNodeSetInfo(  spilled_nodes  );
	coalesced_nodesI   = new BackupNodeSetInfo(  coalesced_nodes  );
	colored_nodesI	   = new BackupNodeSetInfo(  colored_nodes  );
	select_stackI	   = new BackupNodeSetInfo(  select_stack  );
    }
    private void restoreNodeSets() {
	// helper array to make traversal code easy
	BackupNodeSetInfo[] infos = new BackupNodeSetInfo[]{
	    initialI,
	    simplify_worklistI, freeze_worklistI,
	    spill_worklistI, spilled_nodesI,
	    coalesced_nodesI, colored_nodesI,
	    select_stackI 
	};

	// must clear all origSets before restoring them
	for(int i=0; i<infos.length; i++)
	    infos[i].clearOrig();
	for(int i=0; i<infos.length; i++)
	    infos[i].restore();
    }

    NodePairSet lastAdjSet;
    private void saveNodePairSet() { lastAdjSet = adjSet.copy(); }
    private void restoreNodePairSet() { adjSet = lastAdjSet.copy(); }

    abstract static class NodeIter {
	public abstract boolean hasNext();
	public abstract Node next();
    }
    NodeIter combine(final NodeIter[] iters) {
	return new NodeIter() {
		int i=0;
		public boolean hasNext() { 
		    while(i < iters.length) {
			if (iters[i].hasNext()) {
			    return true;
			} else {
			    i++;
			    continue;
			}
		    }
		    return false;
		}
		public Node next() { 
		    hasNext(); 
		    return iters[i].next(); }
	    };
    }

    final class NodeSet  {
	String name; // for debugging
	int size;
	Node head; // dummy element

	void checkRep() { if (true) return;
	    Util.assert(size >= 0);
	    Util.assert(head != null);
	    Node curr = head;
	    int sz = -1;
	    do { 
		Util.assert( curr.s_prev.s_next == curr );
		Util.assert( curr.s_next.s_prev == curr );
		sz++;
		curr = curr.s_next;
	    } while (curr != head);
	    Util.assert(sz == size);
	}


	NodeSet(String s) { 
	    name = s; 
	    head = new Node(null); 
	    size = 0; 
	    checkRep(); 
	}

	NodeIter iter() { 
	    checkRep();
	    return new NodeIter() {
		    Node curr = head;
		    public boolean hasNext() { return curr.s_next != head; }
		    public Node next() { checkRep();
		    Util.assert(hasNext());
		    Node ret = curr.s_next; curr = ret; 
		    checkRep();
		    return ret; 
		    }
		};
	}

	Node pop() {
	    checkRep(); 
	    Node n = head.s_next; 
	    remove(n); 
	    checkRep(); 
	    return n;
	}

	boolean isEmpty() { return size == 0; }
	void clear() { while( !isEmpty() ) pop(); }

	void remove(Node n) {
	    checkRep();
	    Util.assert(n.s_rep == this, 
			this.name + 
			" tried to remove a node that is in "+
			n.s_rep.name);

	    Util.assert(! n.locked, "node "+n+" should not be locked" );

	    n.s_prev.s_next = n.s_next;
	    n.s_next.s_prev = n.s_prev;
	    size--;
	    n.s_next = n;
	    n.s_prev = n;
	    n.s_rep = null;

	    checkRep();
	}
	void add(Node n) {
	    // If select_stack is going to be implemented as a
	    // NodeSet, .add(..) needs to "push" Nodes on FRONT of
	    // set, not the end.

	    checkRep();
	    Util.assert(n.s_prev == n);
	    Util.assert(n.s_next == n);	    
	    Util.assert(n.s_rep == null);

	    Util.assert(! n.locked, "node "+n+" should not be locked" );

	    head.s_next.s_prev = n;
	    n.s_next = head.s_next;
	    n.s_prev = head;
	    head.s_next = n;
	    n.s_rep = this;
	    size++;

	    n.nodeSet_history.addFirst(this);

	    checkRep();
	}

	Set asSet() { return new java.util.AbstractSet() {
		public int size() { return size; }
		public Iterator iterator() {
		    final NodeIter ni = iter();
		    return new harpoon.Util.UnmodifiableIterator() {
			    public boolean hasNext() {return ni.hasNext();}
			    public Object next() {return ni.next();}
			};
		}
	    };
	}
	
	public String toString() { 
	    return "NodeSet< "+name+", "+asSet()+" >"; }

    }

    NodeIter adjacent(Node n) { 
	final NodeIter internIter = n.neighbors.iter();
	// filter out ( selectStack U coalescedNodes )
	return new NodeIter() {
		Node next = null;
		public boolean hasNext() {
		    if (next == null) {
			while(internIter.hasNext()) {
			    next = internIter.next();
			    if (next.isSelectStack() || next.isCoalesced()) {
				continue;
			    } else {
				return true;
			    }
			}
			return false;
		    } else {
			return true;
		    }
		}
		public Node next() {
		    hasNext();
		    Node rtrn = next; 
		    next = null; 
		    return rtrn;
		}
	    };
    }

    final static class NodePairSet {
	HashSet pairs = new HashSet();
	public boolean contains(Node a, Node b) { 
	    return pairs.contains(Default.pair(a,b));
	}
	public void add(Node a, Node b) { 
	    pairs.add(Default.pair(a, b)); 
	}
	public NodePairSet copy() {
	    NodePairSet set = new NodePairSet();
	    set.pairs = (HashSet) pairs.clone();
	    return set;
	}
    }

    final static class NodeList { 
	final static class Cons { Node elem; Cons next;  }
	int size = 0;
	Cons first, last;
	NodeList() {}
	NodeList(List/*Node*/ ns) { 
	    for(Iterator i=ns.iterator(); i.hasNext();)
		add( (Node) i.next() );
	}
	void add(Node n) {
	    Cons c = new Cons();
	    c.elem = n;
	    if (first == null) {
		first = last = c;
	    } else {
		c.next = first;
		first = c;
	    }
	    size++;
	}
	/** INVALIDATES 'l' */
	void append(NodeList l) {
	    last.next = l.first;
	    last = l.last;
	    l.first = null; l.last = null;
	    size += l.size;
	}
	void checkRep() {
	    Util.assert(last.next == null);
	    for(Cons curr = first; curr != last; curr = curr.next ) 
		Util.assert(curr != null);
	}
	NodeIter iter() {
	    return new NodeIter() {
		    Cons c = first;
		    public boolean hasNext() { return c != null; }
		    public Node next() {
			Node n = c.elem; c = c.next; return n;
		    }
		};
	}
	List toList() {
	    ArrayList l = new ArrayList(size);
	    for(NodeIter i=iter(); i.hasNext();)
		l.add(i.next());
	    return l;
	}
	public String toString() { return "NodeList< "+toList()+" >"; }
    }

    int nextId = 1;
    ArrayList allNodes = new ArrayList();
    final class Node {
	final int id;

	// prev and next pointers for this node's set
	Node s_prev, s_next;

	// Set Representative
	NodeSet s_rep;

	// the degree of this in the interference graph
	int degree = 0;

	// linked list of neighbors of this in interference graph
	NodeList neighbors = new NodeList();

	// when a move (u,v) has been coalesced and v put into
	// coalescedNodes, then v.alias = u
	Node alias = null;

	// list of moves this node is associated with
	MoveList moves = new MoveList();

	// color of this, ( 0 <= color < K when assigned )
	int color = -1;

	// Web for this (null if this is dummy header element)
	final Web web; 

	// for debugging purposes: a sequence of the node sets this
	// has been a member of during its lifetime
	java.util.LinkedList nodeSet_history = new java.util.LinkedList();

	// special case for dummy nodes (for which 'w == null')
	public Node(Web w) { 
	    id = nextId; nextId++; s_prev = s_next = this; web = w;
	    allNodes.add(this);
	} 

	public Node(NodeSet which, Web w) { 
	    this(w); 
	    which.add(this); 
	}

	boolean locked = false;

	// machine registers, preassigned a color
	public boolean isPrecolored()      { 
	    boolean r = s_rep == precolored;        
	    if (r) Util.assert(color != -1);
	    return r;
	}

	// temporary registers, not precolored and not yet processed 
	public boolean isInitial()       { return s_rep == initial; }

	// list of low-degree non-move-related nodes
	public boolean isSimplifyWorkL() { return s_rep == simplify_worklist;}

	// low-degree move-related nodes
	public boolean isFreezeWorkL()   { return s_rep == freeze_worklist;  }

	// high-degree nodes
	public boolean isSpillWorkL()    { return s_rep == spill_worklist;   }

	// nodes marked for spilling during this round
	public boolean isSpilled()       { return s_rep == spilled_nodes;    }

	// registers that have been coalesced; when u <- v is
	// coalesced, v is added ot this set and u put back on some
	// work-list (or vice versa)
	public boolean isCoalesced()     { return s_rep == coalesced_nodes;  }

	// nodes successfully colored
	public boolean isColored() { 
	    boolean r = s_rep == colored_nodes;     
	    if (r) Util.assert(color != -1); 
	    return r;
	}

	// nodes on select stack
	public boolean isSelectStack()   { return s_rep == select_stack;     }

	public String toString() {
	    return "Node<"+
		"id:"+id+
		", deg:"+degree+
		", alias:"+alias+
		", temp:"+((web==null)?"none":web.temp+"")+
		// ", history:"+nodeSet_history+
		">";
	}
    }

    static class ColorSet {
	boolean[] colors;
	int size;
	public ColorSet(int numColors) {
	    size = numColors;
	    colors = new boolean[numColors];
	    for(int i=0; i < colors.length; i++) {
		colors[i] = true;
	    }
	}
	/** requires: ! this.isEmpty() */
	public int available() {
	    for(int i=0; i<colors.length; i++) {
		if (colors[i]) 
		    return i;
	    }
	    throw new RuntimeException();
	}
	public void remove(int color) {
	    if (color < colors.length) {
		if ( colors[ color ] )
		    size--;
		colors[ color ] = false;
	    } else {
		// removing an unused register-only color; NOP
	    }
	}
	public boolean isEmpty() { return size == 0; }
    }




    final class Move {
	Collection dsts() { return Collections.singleton(dst); }
	Collection srcs() { return Collections.singleton(src); }
	Node dst, src;
	Move s_prev, s_next;
	MoveSet s_rep; // Set Representative
	Instr instr = null; // null for dummy header Moves

	// dummy ctor
	Move() { s_prev = s_next = this; }

	Move(Instr i, Node dest, Node source) {
	    this();
	    instr = i;
	    this.dst = dest;
	    this.src = source;
	}
	public boolean isCoalesced()   { return s_rep == coalesced_moves; }
	public boolean isConstrained() { return s_rep == constrained_moves; }
	public boolean isFrozen()      { return s_rep == frozen_moves; }
	public boolean isWorklist()    { return s_rep == worklist_moves; }
	public boolean isActive()      { return s_rep == active_moves; }

	public String toString() {
	    // remove package info cruft from identity-based String
	    String s = super.toString();
	    int i = s.indexOf("Move");
	    return 
		s.substring(i)+
		" set:"+s_rep+
		" instr:"+instr;
	}
    }

    MoveSet coalesced_moves;
    MoveSet constrained_moves;
    MoveSet frozen_moves;
    MoveSet worklist_moves;
    MoveSet active_moves;
    void checkMoveSets() {
	if( ! CHECK_INV ) 
	    return;

	MoveSet[] sets = new MoveSet[]{
	    coalesced_moves,constrained_moves,
	    frozen_moves,worklist_moves,
	    active_moves
	};
	for(int i=0; i<sets.length; i++) 
	    sets[i].checkRep();
    }

    // these sets collectively partition the space of Moves
    void initializeMoveSets() {
	coalesced_moves   = new MoveSet("coalesced_moves");
	constrained_moves = new MoveSet("constrained_moves");
	frozen_moves      = new MoveSet("frozen_moves");
	worklist_moves    = new MoveSet("worklist_moves");
	active_moves      = new MoveSet("active_moves");
    }

    private void resetMoveSets() {
	// all sets except worklist_moves
	MoveSet[] sets = new MoveSet[]{ 	
	    coalesced_moves,constrained_moves,
	    frozen_moves,active_moves
	};
	for(int i=0; i<sets.length; i++) 
	    while( ! sets[i].isEmpty() ) {
		sets[i].checkRep();
		worklist_moves.add( sets[i].pop() );
	    }
    }

    final class MoveSet { 
	private int size;
	private Move head; // dummy element
	final String name;
	MoveSet(String name) { 
	    this.name = name;
	    head = new Move(); 
	    add_no_check_rep(head); 
	    size = 0;
	    checkRep();
	}
	
	void checkRep() {
	    if( ! CHECK_INV )
		return;

	    Move curr = head;
	    int checkSize = -1;
	    do {
		checkSize++;
		Util.assert( curr.s_next.s_prev == curr );
		Util.assert( curr.s_next.s_prev == curr );
		Util.assert( curr.s_rep == this );
		curr = curr.s_next;
	    } while(curr != head);
	    if (CHECK_INV)
	    Util.assert( size == checkSize, 
			 "size should be "+checkSize+" not "+size );
	}
	Iterator iter() {
	    return new harpoon.Util.UnmodifiableIterator() {
		    Move curr = head;
		    public boolean hasNext() { return curr.s_next != head; }
		    public Object next() {
			Move n = curr.s_next;
			curr = curr.s_next;
			return n;
		    }
		};
	}
	boolean isEmpty() { return size == 0; }
	Move pop() { 
	    Util.assert(!isEmpty(), "should not be empty");
	    Move n = head.s_next; 
	    if (CHECK_INV)
	    Util.assert(n!=head, "should not return head, "+
			"size:"+size+" set:"+asSet()); 
	    remove(n); 
	    return n; 
	}
	
	void remove(Move n) {
	    checkRep();
	    if (CHECK_INV)
	    Util.assert(n.s_rep == this, 
			"called "+this+".remove(..) "+
			"on move:"+n+" in "+n.s_rep );
	    Util.assert(size != 0);
	    n.s_prev.s_next = n.s_next;
	    n.s_next.s_prev = n.s_prev;
	    n.s_rep = null; n.s_prev = null; n.s_next = null;
	    size--;
	    checkRep();
	}
	void add(Move n) {
	    checkRep();
	    add_no_check_rep(n);
	    checkRep();
	}
	private void add_no_check_rep(Move n){
	    Move prev = head.s_prev;
	    prev.s_next = n;
	    n.s_prev = prev; n.s_next = head;
	    n.s_rep = this;
	    head.s_prev = n;
	    size++;
	}
	Set asSet() {
	    HashSet rtn = new HashSet();
	    for(Iterator iter=iter(); iter.hasNext();) 
		rtn.add(iter.next());
	    return rtn;
	}
	public String toString() { return "MoveSet< "+name+", "+asSet()+" >"; }
    }
    static final class MoveList {
	final static class Cons { Move elem; Cons next; }
	Cons first;
	int size = 0;
	boolean isEmpty() {
	    return size == 0;
	}
	MoveList() {}
	MoveList(List/*Move*/ ms) {
	    for(Iterator i=ms.iterator(); i.hasNext();)
		add( (Move) i.next() );
	}
	void add(Move m) {
	    Cons c = new Cons();
	    c.elem = m;
	    if (first == null) {
		first = c;
	    } else {
		c.next = first;
		first = c;
	    }
	    size++;
	}
	/** INVALIDATES 'l' */
	void append(MoveList l) {
	    Cons last = first;
	    while(last.next != null) 
		last = last.next;

	    last.next = l.first;
	    size += l.size;

	    l.first = null;
	    l.size = -1;
	}
	Iterator iterator() {
	    return new UnmodifiableIterator() {
		    Cons c = first;
		    public boolean hasNext() { return c != null; }
		    public Object next() { Move m=c.elem; c=c.next; return m;}
		};
	}
	public List toList() {
	    java.util.ArrayList l = new java.util.ArrayList();
	    for(Iterator m=iterator(); m.hasNext(); ){
		l.add(m.next());
	    }
	    return l;
	}
	public String toString() { return "MoveList< "+toList()+" >"; }
    }


}
