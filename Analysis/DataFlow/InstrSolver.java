// InstrSolver.java, created Wed Apr  7 22:37:59 1999 by pnkfelix
// Copyright (C) 1999 Felix S Klock <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.DataFlow;

import harpoon.Util.WorkSet;

import java.util.Set;
import java.util.Iterator;
import java.util.Collections;
/**
 * <code>InstrSolver</code>
 * 
 * @author  Felix S Klock <pnkfelix@mit.edu>
 * @version $Id: InstrSolver.java,v 1.1.2.4 1999-06-24 02:02:39 pnkfelix Exp $
 */
public final class InstrSolver  {
    
    private static final boolean DEBUG = false;

    /** Overriding default constructor to prevent instantiation. */
    private InstrSolver() {
        
    }

    /** Performs dataflow analysis on a set of
	<code>BasicBlock</code>s.  Finds a maximum fixed point for the
	data-flow equations defined by <code>v</code>.
	
	<BR> <B>requires:</B> 
	     <OL>
	     <LI>Every instruction in <code>root</code> and in the
	         <code>BasicBlock</code>s linked to by
		 <code>root</code> is an instance of an
		 <code>Instr</code>.  
	     <LI><code>v</code> can visit instructions of type
   	         <code>Instr</code>. 
	     </OL>
	<BR> <B>modifies:</B> <code>v</code>
	<BR> <B>effects:</B> 
             Sends <code>v</code> into <code>root</code> and the
	     <code>BasicBlock</code>s linked to by <code>root</code>,
	     performing <code>v</code>'s transfer function on each
	     <code>BasicBlock</code> in turn, tracking for when no
	     change occurs, at which point the analysis is complete.
	     Note that while this method guarantees that
	     <code>root</code> will be visited by <code>v</code>,
	     there is no guarantee on the number of times, if at all,
	     <code>v</code> will visit the siblings of
	     <code>root</code>, and therefore the analysis may
	     terminate prematurely.
     */
    public static void worklistSolver(BasicBlock root, DataFlowBasicBlockVisitor v) {
	WorkSet w = new WorkSet();
	w.push(root);
	while (!w.isEmpty()) {
	    // v.changed = false;
	    BasicBlock b = (BasicBlock) w.pull();
	    if (DEBUG) 
		System.out.println
		    ("Visiting block " + b);
	    b.visit(v);
	    v.addSuccessors(w, b);
	}
    } 


    /** Performs dataflow analysis on a set of
	<code>BasicBlock</code>s.  Finds a maximum fixed point for the
	data-flow equations defined by <code>v</code>.
	
	<BR> <B>requires:</B> 
             <OL>
	     <LI> The elements of <code>iter</code> are
	          <code>BasicBlock</code>s.
	     <LI> Every instruction in the elements of
	          <code>iter</code> and in the
		  <code>BasicBlock</code>s linked to by the elements
		  of <code>iter</code> is an instance of an
		  <code>Instr</code>.   
	     <LI> <code>v</code> can visit instructions of type
	          <code>Instr</code>. 
	     </OL>
	<BR> <B>modifies:</B> <code>v</code>, <code>iter</code>
	<BR> <B>effects:</B> 
             Sends <code>v</code> into the elements of
	     <code>iter</code> and the <code>BasicBlock</code>s
	     linked to by the elements of <code>iter</code>,
	     performing <code>v</code>'s transfer function on each 
	     <code>BasicBlock</code> in turn, tracking for when no
	     change occurs, at which point the analysis is complete.
	     This method guarantees that all of the
	     <code>BasicBlock</code>s in <code>iter</code> will be
	     visited by <code>v</code> at least once.  */
    public static void worklistSolver(Iterator iter, DataFlowBasicBlockVisitor v) {
	WorkSet w = new WorkSet();
	while (iter.hasNext()) w.push(iter.next());
	while (!w.isEmpty()) {
	    // v.changed = false;
	    BasicBlock b = (BasicBlock) w.pull();
	    if (DEBUG) 
		System.out.println
		    ("Visiting block " + b);
	    b.visit(v);
	    v.addSuccessors(w, b);
	}
    } 
    
}
