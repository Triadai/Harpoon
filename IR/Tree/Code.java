// Code.java, created Mon Feb  8 16:55:15 1999 by duncan
// Copyright (C) 1998 Duncan Bryce <duncan@lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.Analysis.Maps.Derivation;
import harpoon.Analysis.Maps.Derivation.DList;
import harpoon.Analysis.Maps.TypeMap;
import harpoon.Backend.Generic.Frame;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HMethod;
import harpoon.IR.Properties.CFGrapher; 
import harpoon.Util.ArrayFactory;
import harpoon.Util.Util;
import harpoon.Temp.LabelList;
import harpoon.Temp.Temp;
import harpoon.Temp.TempFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

/**
 * <code>Tree.Code</code> is an abstract superclass of codeviews
 * using the components in <code>IR.Tree</code>.  It implements
 * shared methods for the various codeviews using <code>Tree</code>s.
 * 
 * @author  Duncan Bryce <duncan@lcs.mit.edu>
 * @version $Id: Code.java,v 1.1.2.42 2000-02-08 23:31:02 cananian Exp $
 */
public abstract class Code extends HCode {
    /** The Tree Objects composing this code view. */
    protected Tree tree;

    /** The Frame containing machine-specific information*/
    protected /* final */ Frame frame;

    /** Tree factory. */
    protected /* final */ TreeFactory tf;

    /** The method that this code view represents. */
    protected /* final */ HMethod parent;

    /** Create a proper TreeFactory. */
    public class TreeFactory extends harpoon.IR.Tree.TreeFactory {
	private int id=0;
	private final TempFactory tempf;
	TreeFactory(String scope) { this.tempf = Temp.tempFactory(scope); }
	public TempFactory tempFactory() { return tempf; }
	/** Returns the <code>HCode</code> to which all <code>Exp</code>s and
	 *  <code>Stm</code>s generated by this factory belong. */
	public Code getParent() { return Code.this; }
	/** Returns the <code>HMethod</code> to which all <code>Exp</code>s and
	 *  <code>Stm</code>s generated by this factory belong. */
	public HMethod getMethod() { return Code.this.getMethod(); }
	public Frame getFrame() { return Code.this.frame; } 
	synchronized int getUniqueID() { return id++; }
	public String toString() { 
	    return "Code.TreeFactory["+getParent().toString()+"]"; 
	}
	public int hashCode() { return Code.this.hashCode(); }
    }

    /** constructor. */
    protected Code(final HMethod parent, final Tree tree, 
		   final Frame frame) {
	final String scope = parent.getDeclaringClass().getName() + "." +
	    parent.getName() + parent.getDescriptor() + "/" + getName();
	this.parent = parent;
	this.tree   = tree;
	this.frame  = frame;
	this.tf     = new TreeFactory(scope);
    }
  
    /** Clone this code representation. The clone has its own copy
     *  of the Tree */
    public abstract HCode  clone(HMethod newMethod, Frame frame);

    /** Returns a means to externally associate control flow with this
     *  tree code.  If this tree code is modified subsequent to a call
     *  to <code>getGrapher()</code>, the grapher is invalid, this method
     *  should be re-invoked to acquire a new grapher.  
     */ 
    public CFGrapher getGrapher() { return new TreeGrapher(this); }

    /** Return the name of this code view. */
    public abstract String getName();
    
    /** Return the <code>HMethod</code> this codeview
     *  belongs to.  */
    public HMethod getMethod() { return this.parent; }

    public Frame getFrame() { return this.frame; }

    public abstract TreeDerivation getTreeDerivation();

    /** Returns the root of the Tree */
    public HCodeElement getRootElement() { 
	// Ensures that the root is a SEQ, and the first instruction is 
	// a SEGMENT.
	Tree first = (SEQ)this.tree;
	while(first.kind()==TreeKind.SEQ) first = ((SEQ)first).getLeft(); 
	Util.assert(first.kind()==TreeKind.SEGMENT); 
	return this.tree; 
    }

    /** Returns the leaves of the Tree */
    public HCodeElement[] getLeafElements() { 
	Stack nodes  = new Stack();
	List  leaves = new ArrayList();

	nodes.push(getRootElement());
	while (!nodes.isEmpty()) {
	    Tree t = (Tree)nodes.pop();
	    if (t.kind()==TreeKind.SEQ) {
		SEQ seq = (SEQ)t;
		if (seq.getLeft()==null) {
		    if (seq.getRight()==null) { leaves.add(seq); }
		    else                 { nodes.push(seq.getRight());  }
		}
		else {
		    nodes.push(seq.getLeft());
		    if (seq.getRight()!=null) nodes.push(seq.getRight());
		}
	    }
	    else if (t.kind()==TreeKind.ESEQ) {
		ESEQ eseq = (ESEQ)t;
		if (eseq.getExp()==null) {
		    if (eseq.getStm()==null) { leaves.add(eseq); }
		    else                { nodes.push(eseq.getStm());    }
		}
		else {
		    nodes.push(eseq.getExp());
		    if (eseq.getStm()!=null) nodes.push(eseq.getStm());
		}
	    }
	    else {
		ExpList explist = t.kids();
		if (explist==null) leaves.add(t);
		else {
		    while (explist!=null) {
			if (explist.head!=null) nodes.push(explist.head);
			explist = explist.tail;
		    }
		}
	    }
	}
	return (HCodeElement[])leaves.toArray(new HCodeElement[0]);
    }
  
    /**
     * Returns an ordered list of the <code>Tree</code> Objects
     * making up this code view.  The root of the tree
     * is in element 0 of the array.
     */
    public HCodeElement[] getElements() {
	return super.getElements();
    }

    /** 
     * Returns an <code>Iterator</code> of the <code>Tree</code> Objects 
     * making up this code view.  The root of the tree is the first element
     * of the Iterator. 
     */
    public Iterator getElementsI() { 
	return new Iterator() {
	    Set h = new HashSet();
	    Stack stack = new Stack(); 
	    { visitElement(getRootElement()); }
	    public boolean hasNext() { 
		if (stack.isEmpty()) {
		    h = null; // free up some memory
		    return false;
		}
		else return true;
	    }
	    public Object next() {
		Tree t;
		if (stack.isEmpty()) throw new NoSuchElementException();
		else {
		    t = (Tree)stack.pop();
		    // Push successors on stack
		    switch (t.kind()) { 
		    case TreeKind.SEQ: 
			SEQ seq = (SEQ)t;
			if (seq.getLeft()!=null)  visitElement(seq.getLeft());
			if (seq.getRight()!=null) visitElement(seq.getRight());
			break;
		    case TreeKind.ESEQ: 
			ESEQ eseq = (ESEQ)t;
			if (eseq.getExp()!=null) visitElement(eseq.getExp());
			if (eseq.getStm()!=null) visitElement(eseq.getStm());
			break;
		    default:
			ExpList explist = t.kids();
			while (explist!=null) {
			    if (explist.head!=null) visitElement(explist.head);
			    explist = explist.tail;
			}
		    }
		}
		return t;
	    }
	    public void remove() { 
		throw new UnsupportedOperationException();
	    }
	    private void visitElement(Object elem) {
		if (h.add(elem)) stack.push(elem);
	    }
	};
    }
  
    // implement elementArrayFactory which returns Tree[]s.  
    public ArrayFactory elementArrayFactory() { return Tree.arrayFactory; }

    public void print(java.io.PrintWriter pw) {
	Print.print(pw,this);
    } 

    /** 
     * Returns true if this codeview is a canonical representation
     */
    public abstract boolean isCanonical();

    /** 
     * Removes the specified <code>Stm</code> from the tree in which it 
     * resides.
     * 
     * <br><b>Requires:</b>
     * <ol>
     *   <li><code>stm</code> is not of type <code>SEQ</code>. 
     *   <li><code>stm.getParent()</code> must be of type <code>SEQ</code>. 
     *   <li><code>stm</code> is an element of this codeview. 
     *   <li><code>stm</code> is not the root element of this codeview.
     * </ol>
     * <br><b>Effects:</b>
     *   Removes <code>stm</code> from this tree.
     */
    public void remove(Stm stm) { 
	Util.assert(stm.kind() != TreeKind.SEQ); 
	Util.assert(stm.getFactory() == this.tf); 
	Util.assert(this.tf.getParent() == this); 
	
	// All predecessors in canonical tree form must be SEQs
	SEQ pred = (SEQ)stm.getParent();
	Stm newPred;

	if      (pred.getLeft()==stm)  { newPred = pred.getRight(); } 
	else if (pred.getRight()==stm) { newPred = pred.getLeft(); } 
	else { throw new Error("Invalid tree form!"); }

	if (pred.getParent() == null) { 
	    // Pred has no parents, it must be the root of the tree. 
	    Util.assert(pred == this.tree); 
	    this.tree         = newPred; 
	    this.tree.parent  = null;
	    this.tree.sibling = null; 
	}
	else { 
	    this.replace(pred, newPred); 
	}
    }
    
    /**
     * Modifies this codeview directly by replacing
     * <code>tOld</code> with <code>tNew</code>.  
     * 
     * <br><b>Requires:</b>
     * <ol>
     *   <li><code>tNew</code>'s type is compatible with its
     *       position in the tree.  For instance, if <code>tOld</code>
     *       is a pointer to the exception handling code of some
     *       <code>CALL</code> object, then <code>tOld</code> must 
     *       be of type <code>NAME</code>. 
     *   <li><code>tOld</code> is an element of this codeview. 
     *   <li><code>tNew</code> was created with this Code's
     *       <code>TreeFactory</code>. 
     * </ol>
     * <br><b>Effects:</b>
     *   Makes the necessary modifications to replace <code>tOld</code>
     *   with <code>tNew</code> in this tree structure. 
     * 
     */ 
    public void replace(Tree tOld, Tree tNew) { 
	Util.assert(tOld.getFactory() == this.tf); 
	Util.assert(tNew.getFactory() == this.tf); 
	Util.assert(this.tf.getParent() == this); 

	if (tOld == this.tree) { // tOld is the root node
	    this.tree = tNew; 
	}
	else { // tOld is not the root.  Use the Replacer visitor. 
	    new Replacer(tOld, tNew); 
	}
    }

    /**
     * Visitor class to implement direct replacement of
     * on Tree with another.  
     */ 
    private static class Replacer extends TreeVisitor { 
	private Tree tOld, tNew; 

	/**
	 * Class constructor. 
	 * @param tOld  the Tree object to be replaced. 
	 * @param tNew  the Tree object to replace tOld with. 
	 */ 
	public Replacer(Tree tOld, Tree tNew) { 
	    this.tOld = tOld;
	    this.tNew = tNew; 

	    Tree parent = tOld.getParent(); 
	    Util.assert(parent != null); 
	    // Update the parent's references. 
	    parent.accept(this); 
	}

	public void visit(BINOP e) { 
	    if      (e.getLeft() == this.tOld)  { e.setLeft((Exp)this.tNew); } 
	    else if (e.getRight() == this.tOld) { e.setRight((Exp)this.tNew); }
	    else { this.errCorrupt(); } 
	} 

	public void visit(CALL e) { 
	    if      (e.getRetex() == this.tOld) { e.setRetex((TEMP)this.tNew); }
 	    else if (e.getHandler() == this.tOld) { e.setHandler((NAME)this.tNew); } 
	    else { this.visit((INVOCATION)e); } 
	}

	public void visit(CJUMP e) { 
	    if (e.getTest() == this.tOld) { e.setTest((Exp)this.tNew); } 
	    else { this.errCorrupt(); } 
	}

	/** A CONST cannot be the parent of another node. */ 
	public void visit(CONST e) { this.errLeaf(e); }

	public void visit(ESEQ e) {
	    if      (e.getStm() == this.tOld) { e.setStm((Stm)this.tOld); } 
	    else if (e.getExp() == this.tOld) { e.setExp((Exp)this.tOld); } 
	    else { this.errCorrupt(); } 
	}

	public void visit(INVOCATION e) { 
	    if (e.getFunc() == this.tOld) { e.setFunc((Exp)this.tNew); } 
	    else if (e.getRetval() == this.tOld) { e.setRetval((TEMP)this.tNew); } 
	    else { 
		ExpList newArgs = 
		    ExpList.replace(e.getArgs(), (Exp)this.tOld, (Exp)this.tNew); 
		e.setArgs(newArgs); 
	    }
	}

	public void visit(JUMP e) { 
	    if (e.getExp() == this.tOld) { e.setExp((Exp)this.tNew); } 
	    else { this.errCorrupt(); } 
	}

	/** A LABEL cannot be the parent of another node. */ 
	public void visit(LABEL e) { this.errLeaf(e); } 

	public void visit(MEM e) { 
	    if (e.getExp() == this.tOld) { e.setExp((Exp)this.tNew); } 
	    else { this.errCorrupt(); } 
	}

	public void visit(METHOD e) {
	    // tOld must be one of the METHOD parameters. 
	    TEMP[] params = e.getParams(); 
	    TEMP[] newParams = new TEMP[params.length]; 
	    System.arraycopy(params, 0, newParams, 0, params.length); 

	    for (int i=0; i<params.length; i++) { 
		if (params[i] == this.tOld) { 
		    newParams[i] = (TEMP)this.tNew; 
		    e.setParams(newParams); 
		    return; 
		}
	    }
	    this.errCorrupt(); 
	}

	public void visit(MOVE e) { 
	    if      (e.getSrc() == this.tOld) { e.setSrc((Exp)this.tNew); } 
	    else if (e.getDst() == this.tOld) { e.setDst((Exp)this.tNew); } 
	    else { this.errCorrupt(); } 
	}

	/** A NAME cannot be the parent of another node. */ 
	public void visit(NAME e) { this.errLeaf(e); } 

	public void visit(RETURN e) {
	    if (e.getRetval() == this.tOld) { e.setRetval((Exp)this.tNew); } 
	    else { this.errCorrupt(); } 
	}

	/** A SEGMENT cannot be the parent of another node. */ 
	public void visit(SEGMENT e) { this.errLeaf(e); }

	public void visit(SEQ e) {
	    if      (e.getLeft() == this.tOld)  { e.setLeft((Stm)this.tNew); } 
	    else if (e.getRight() == this.tOld) { e.setRight((Stm)this.tNew); }
	    else { this.errCorrupt(); } 
	}

	/** A TEMP cannot be the parent of another node. */
	public void visit(TEMP e) { this.errLeaf(e); } 

	public void visit(THROW e) {
	    if      (e.getRetex() == this.tOld)   { e.setRetex((Exp)this.tNew); } 
	    else if (e.getHandler() == this.tOld) { e.setHandler((Exp)this.tNew); } 
	    else { this.errCorrupt(); } 
	}

	public void visit(Tree t) { throw new Error("No defaults here!"); }

	public void visit(UNOP e) {
	    if (e.getOperand() == this.tOld) { e.setOperand((Exp)this.tNew); } 
	    else { this.errCorrupt(); } 
	}

	// Utility method:  called when the Tree structure has been
	// corrupted. 
	private void errCorrupt() { 
	    throw new Error("The tree structure has been corrupted."); 
	}

	// Utility method:  called when a leaf node is being treated
	// as a non-leaf node. 
	private void errLeaf(Tree t) { 
	    throw new Error
		("Attempted to treat: " + t + " as a non-leaf node."); 
	}
    }
}


