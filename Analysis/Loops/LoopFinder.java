// LoopFinder.java, created Tue Jun 15 23:15:07 1999 by bdemsky
// Licensed under the terms of the GNU GPL; see COPYING for details.
// Copyright 1999 by Brian Demsky

package harpoon.Analysis.Loops;

import harpoon.Analysis.Loops.Loops;
import harpoon.Util.WorkSet;
import harpoon.Analysis.DomTree;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeEdge;
import harpoon.ClassFile.HCodeElement;
import harpoon.IR.Properties.CFGraphable;
import harpoon.Util.Util;

import java.util.Set;
import java.util.Hashtable;
import java.util.Iterator;
/**
 * <code>LoopFinder</code> implements Dominator Tree Loop detection.
 * 
 * @author  Brian Demsky <bdemsky@mit.edu>
 * @version $Id: LoopFinder.java,v 1.1.2.9 2000-11-10 21:57:21 cananian Exp $
 */

public class LoopFinder implements Loops {
    
    DomTree dominator;
    HCode hc,lasthc;
    WorkSet setofloops;
    Loop root;
    Loop ptr;
    

    /** Creates a new LoopFinder object. 
      * This call takes an HCode and returns a LoopFinder object
      * at the root level.  Only use with Objects implementing the
      * CFGraphable interface!
      *<BR> <B> Requires: </B> <code> hc </code> is a <code> HCode</code> implementing
      *        the <code> CFGraphable </code> interface.*/
    
    public LoopFinder(HCode hc) {
	this.hc=hc;
	this.dominator=new DomTree(hc, false);
	analyze();
	this.ptr=root;    
    }
    
    /**This method is for internal use only.
     *It returns a Loopfinder object at any level,
     *but it doesn't regenerate the internal tree
     *so any external calls would result in garbage.*/
    
    private LoopFinder(HCode hc, DomTree dt, Loop root, Loop ptr) {
	this.lasthc=hc;
	this.hc=hc;
	this.dominator=dt;
	this.root=root;
	this.ptr=ptr;
    }
    
    /*-----------------------------*/
    
    /**  This method returns the Root level loop for a given <code>HCode</code>.
     *  Does the same thing as the constructor call, but for an existing
     *  LoopFinder object.*/
    
    public Loops getRootloop(HCode hc) {
      this.hc=hc;
      analyze();
      return new LoopFinder(hc,dominator,root,root);
    }
    
    /**  This method returns the entry point of the loop.
     *   For the natural loops we consider, that is simply the header. 
     *   It returns a <code>Set</code> of <code>HCodeElements</code>.*/
    
    public Set loopEntrances() {
      analyze();
      WorkSet entries=new WorkSet();
      entries.push(ptr.header);
      return entries;
    }
    
    /**  This method finds all of the backedges of the loop.
     *   Since we combine natural loops with the same header, this
     *   can be greater than one. This method returns a <code>Set</code> of
     *   <code>HCodeElements</code>.*/

    public Set loopBackedges() {
	analyze();
	WorkSet A=new WorkSet();
	Iterator iterate=ptr.entries.iterator();
	while (iterate.hasNext()) {
	    HCodeElement hce=(HCodeElement)iterate.next();
	    for (int i=0;i<((CFGraphable)hce).succ().length;i++) {
		if (((CFGraphable)hce).succ()[i].to()==ptr.header) {
		    A.push(hce);
		    break;
		}
	    }
	}
	return A;
    }
    
    /**  This metho returns all of the exits from a loop.
     *   It returns them in the form <code>Set</code> of <code>HCodeElements</code>.*/
    
    public Set loopExits() {
	analyze();
	WorkSet A=new WorkSet();
	Iterator iterate=ptr.entries.iterator();
	while (iterate.hasNext()) {
	    HCodeElement hce=(HCodeElement)iterate.next();
	    for (int i=0;i<((CFGraphable)hce).succ().length;i++) {
		if (!ptr.entries.contains(((CFGraphable)hce).succ()[i].to())) {
		    A.push(hce);
		    break;
		}
	    }
	}
	return A;
    }
    
    /**Returns a <code>Set</code> with all of the <code>HCodeElement</code>s of the loop and
     *loops included entirely within this loop. */
    
    public Set loopIncelements() {
	analyze();
	WorkSet A=new WorkSet(ptr.entries);
	return A;
    }
    
    /** Returns all of the <code>HCodeElement</code>s of this loop that aren't in a nested
     *  loop. This returns a <code>Set</code> of <code>HCodeElement</code>s.*/
    
    public Set loopExcelements() {
	analyze();
	WorkSet A=new WorkSet(ptr.entries);
	WorkSet todo=new WorkSet();
	//Get the children
	Iterator iterat=ptr.children.iterator();
	while (iterat.hasNext())
	    todo.push(iterat.next());
	//Go down the tree
	while(!todo.isEmpty()) {
	    Loop currptr=(Loop)todo.pop();
	    Iterator iterate=currptr.children.iterator();
	    while (iterate.hasNext()) {
		todo.push(iterate.next());
	    }
	    iterate=currptr.entries.iterator();
	    while (iterate.hasNext())
		A.remove(iterate.next());
	}
	return A;
    }
    
    /** Returns a <code>Set</code> of loops that are nested inside of this loop.*/
    
    public Set nestedLoops() {
	analyze();
	WorkSet L=new WorkSet();
	Iterator iterate=ptr.children.iterator();
	while (iterate.hasNext())
	    L.push(new LoopFinder(hc,dominator,root,(Loop) iterate.next()));
	return L;
    }
    
    /** Returns the <code>Loops</code> that contains this loop.
     *  If this is the top level loop, this call returns a null pointer.*/
    
    public Loops parentLoop() {
	analyze();
	if (ptr.parent!=null)
	    return new LoopFinder(hc,dominator,root,ptr.parent);
	else return null;
    }
    
    /*---------------------------*/
    // public information accessor methods.
    
    /*---------------------------*/
    // Analysis code.
    
    
    /** Main analysis method. */
    
    void analyze() {
	//Have we analyzed this set before?
	//If so, don't do it again!!!
	if (hc!=lasthc) {
	    
	    //Did the caller hand us a bogus object?
	    //If so, throw it something
	    
	    lasthc=hc;
	    
	    //Set up the top level loop, so we can fill it with HCodeElements
	    //as we go along
	    root=new Loop();
	    root.header=hc.getRootElement();
	    
	    //Set up a WorkSet for storing loops before we build the
	    //nested loop tree
	    setofloops=new WorkSet();
	    
	    //Find loops
	    findloopheaders(hc.getRootElement());
	    
	    //Build the nested loop tree
	    buildtree();
	}
    } 
    // end analysis.
    
    void buildtree() {
	//go through set of generated loops
	while(!setofloops.isEmpty()) {
	    
	    //Pull out one
	    Loop A=(Loop) setofloops.pull();
	    
	    //Add it to the tree, complain if oddness
	    if (addnode(A, root)!=1) 
		System.out.println("Evil Error in LoopFinder while building tree.");
	}
    }

    //Adds a node to the tree...Its recursive
    
    int addnode(Loop A, Loop treenode) {

	//Only need to go deeper if the header is contained in this loop
	if (treenode.entries.contains(A.header))
	    
	    //Do we share headers?
	    if (treenode.header!=A.header) {
		
		//No...  Loop through our children to see if they want this
		//node.

		//Use integers for tri-state:
		//0=not stored here, 1=stored and everything is good
		//2=combined 2 natural loops with same header...need cleanup
		
		int stored=0;
		Iterator iterate=treenode.children.iterator();
		Loop temp=new Loop();
		while (iterate.hasNext()) {
		    temp=(Loop) iterate.next();
		    stored=addnode(A,temp);
		    if (stored!=0) break;
		}
		
		//See what our children did for us
		
		if (stored==0) {
		    //We get a new child...
		    treenode.children.push(A);
		    temp=A;
		}
		
		//Need to do cleanup for case 0 or 2
		//temp points to the new child
		
		if (stored!=1) {
		    
		    //Have to make sure that none of the nodes under this one
		    //are children of the new node
		    
		    Iterator iterate2=treenode.children.iterator();
		    temp.parent=treenode;
		    
		    //Loop through the children
		    while (iterate2.hasNext()) {
			Loop temp2=(Loop)iterate2.next();

			//Don't look at the new node...otherwise we will create
			//a unreachable subtree

			if (temp2!=temp)
			    //If the new node has a childs header
			    //give the child up to it...
			    
			    if (temp.entries.contains(temp2.header)) {
				temp.children.push(temp2);
				iterate2.remove();
			    }
		    }
		}
		
		//We fixed everything...let our parents know
		return 1;
	    } else {
		//need to combine loops
		while (!A.entries.isEmpty()) {
		    treenode.entries.push(A.entries.pull());
		}
		//let the previous caller know that they have stuff todo
		return 2;
	    }
	//We aren't adopting the new node
	else return 0;
    }

    void findloopheaders(HCodeElement current_node) {
        //look at the current node
        visit(current_node);
	
        //add it to the all inclusive root loop
        root.entries.push(current_node);

        //See if those we dominate are backedges
        HCodeElement[] children=dominator.children(current_node);
        for (int i=0;i<children.length;i++)
	    findloopheaders(children[i]);
    }


    void visit(HCodeElement q) {
	Loop A=new Loop();
	WorkSet B=new WorkSet();

	//Loop through all of our outgoing edges
	for (int i=0;i<(((CFGraphable)q).succ()).length;i++) {
	    HCodeElement temp=q;
	    
	    //Go up the dominator tree until
	    //we hit the root element or we
	    //find the node we jump back too
	    while ((temp!=(hc.getRootElement()))&&
		   (((CFGraphable)q).succ()[i]).to()!=temp) {
		temp=dominator.idom(temp);
	    }

	    //If we found the node we jumped back to
	    //then build loop

	    if (((CFGraphable)q).succ()[i].to()==temp) {
		
		//found a loop
		A.entries.push(temp); //Push the header
		A.header=temp;
		B.push(q); //Put the backedge in the todo list

		//Starting with the backedge, work on the incoming edges
		//until we get back to the loop header...
		//Then we have the entire natural loop

		while(!B.isEmpty()) {
		    HCodeElement newnode=(HCodeElement)B.pull();
		    
		    //Add all of the new incoming edges that we haven't already
		    //visited
		    for (int j=0;j<((CFGraphable)newnode).pred().length;j++) {
			if (!A.entries.contains(((CFGraphable)newnode).pred()[j].from()))
			    B.push(((CFGraphable)newnode).pred()[j].from());
		    }
		    
		    //push the new node on our list of nodes in the loop
		    A.entries.push(newnode);
		}

		//save our new loop
		setofloops.push(A);
            }
	}
    }

    //Structure for building internal trees...
    
    class Loop {
	public WorkSet entries=new WorkSet();
         public HCodeElement header;
	//Elements of the WorkSet of children are
	//of the type Loop
	public WorkSet children=new WorkSet();
	public Loop parent;
    }
}
