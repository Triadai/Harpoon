// Matching.java, created Fri Feb  4 16:16:27 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.PointerAnalysis;

import java.util.Iterator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>Matching</code> is a wrapper for some functions related to the \
 mapping of nodes due to the interaction of two entities: caller-callee \
 interaction or interthread interaction.<br>

 There are five methods, each one implementing a specific rule:
 <ul>
 <li> Rule 0 was added by my to offer a transitive closure to the mapping
 relation. <br>
 <li> Rule 1 doesn't need to be implemented explicitly: we satisfy it by
 starting with the initial mapping and adding stuff to it without ever
 deleting an existent mapping between two nodes.<br>
 <li> Rules 2 and 3 are like in the original paper.<br>
 <li> Rules 22 and 32 are variations of 2 and 3 introduced by me to handle
 the case when a scope reads an outside edges which is in fact an inside edge
 from the same scope (because a thread running in parallel merged together
 some nodes from the same scope). This rule is the logical companion
 of rule 0.
 </ul>

 All the methods have an identical interface. They are designed to be
 used in an incremental manner: only the new mappings for <code>n1</code>
 are used. The normal way to use them is in a fixed-point loop. The mappings
 are updated according to the inference rules. If new mappings (i.e.
 mappings that didn't exist previously) are generated those mappings are
 also put in <code>new_info</code> so that they can be used in the new
 interation of the loop. 
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: Matching.java,v 1.1.2.4 2000-03-02 04:45:30 salcianu Exp $
 */
abstract class Matching {

    /** Applies rule 0: if there is a mu[i] relation from node1 to node2
	and there is a mu[ib]/mu[i] relation from node2 to node3, then put a
	mu[i] relation from node1 to node2.<br>
	<i>Note</i>: you won't find this rule in the paper; it was added
	by me to fix the algorithm. */
    public static final void rule0(Relation mu[], PAWorkList W[]){
	for(int i=0;i<2;i++){
	    int ib = 1-i;
	    Iterator it_n1 = mu[i].keySet().iterator();
	    while(it_n1.hasNext()){
		PANode n1 = (PANode) it_n1.next();
		Set n1_mappings = mu[i].getValuesSet(n1);

		Set new_n1 = new HashSet();
		Iterator it_n2 = mu[i].getValues(n1);
		while(it_n2.hasNext()){
		    PANode n2 = (PANode) it_n2.next();
		    new_n1.addAll(mu[i].getValuesSet(n2));
		    new_n1.addAll(mu[ib].getValuesSet(n2));
		}
		
		if(n1_mappings.addAll(new_n1))
		    W[i].add(n1);
	    }
	}
    }


    /** Applies rule 0: if there is a mu[i] relation from node1 to node2
	and there is a mu[ib]/mu[i] relation from node2 to node3, then put a
	mu[i] relation from node1 to node2.<br>
	<i>Note</i>: you won't find this rule in the paper; it was added
	by me to fix the algorithm. */
    public static final void rule0(PANode node1, Set new_mappings_for_node,
		   ParIntGraph pig[],
		   PAWorkList W[], Relation mu[], Relation new_info[], 
		   int i, int ib){
	HashSet new_nodes3 = new HashSet();

	// for all the possible instances of node2 from the inference rule ...
	Iterator it2 = new_mappings_for_node.iterator();
	while(it2.hasNext()){
	    PANode node2 = (PANode) it2.next();
	    // ... find all the possible instances of node3 ...
	    Iterator it3 = mu[ib].getValues(node2);
	    while(it3.hasNext()){
		PANode node3 = (PANode) it3.next();
		if(!mu[i].contains(node1,node3))
		    new_nodes3.add(node3); 
	    }

	    it3 = mu[i].getValues(node2);
	    while(it3.hasNext()){
		PANode node3 = (PANode) it3.next();
		if(!mu[i].contains(node1,node3))
		    new_nodes3.add(node3); 
	    }
	}

	System.out.println("rule0 " + node1 + " -> " + new_mappings_for_node);

	if(!new_nodes3.isEmpty()){

	    System.out.println("New mappings: " + node1 + " -> " + new_nodes3);

	    mu[i].addAll(node1,new_nodes3);
	    new_info[i].addAll(node1, new_nodes3);
	    W[i].add(node1);
	}
    }


    /** Applies rule 2: matches an outside edge starting from node1 in 
	pig[i] against an inside edge in pig[ib]. As a result, some possibly
	new mappings for node2 will be generated and put in mu[i].
	This rule is designed to be used in an incremental manner: it works
	only with the new mappings of node1. All the new mappings for node2
	will also be put in new_info[i]. If such new mappings appear, 
	all the instances of node2 are added to W[i].*/
    public static final void rule2(PANode node1, Set new_mappings_for_node1,
		   ParIntGraph pig[],
		   PAWorkList W[], Relation mu[], Relation new_info[], 
		   int i, int ib){
	// nodes3 stands for all the new instances of n3
	// from the inference rule
	Set nodes3 = new_mappings_for_node1;

	Enumeration flags = pig[i].G.O.allFlagsForNode(node1);
	while(flags.hasMoreElements()){
	    String f = (String) flags.nextElement();
	    
	    // nodes2 stands for all the nodes that could play
	    // the role of n2 from the inference rule
	    Set nodes2 = pig[i].G.O.pointedNodes(node1,f);
	    if(nodes2.isEmpty()) continue;
	    
	    // nodes4 stands for all the nodes that could play
	    // the role of n4 from the inference rule
	    Set nodes4 = pig[ib].G.I.pointedNodes(nodes3,f);
	    if(nodes4.isEmpty()) continue;

	    // set up the relation mu[i] from any node from nodes2
	    // to any node from nodes4
	    Iterator it2 = nodes2.iterator();
	    while(it2.hasNext()){
		PANode node2 = (PANode)it2.next();
		boolean changed = false;
		Iterator it4 = nodes4.iterator();
		while(it4.hasNext()){
		    PANode node4 = (PANode)it4.next();
		    if(mu[i].add(node2,node4)){
			changed = true;
			new_info[i].add(node2,node4);
		    }
		}
		// nodes with new info are put in the worklist
		if(changed) W[i].add(node2);
	    }
	}
    }



    // TAKE CARE: THREAD UNSAFE
    // these prealocated objects gain us some efficiency, they are used
    // in rule 22 and rule 32, only to search into the edge ordering relation
    static PAEdge inside_edge  = new PAEdge(null, "", null);
    static PAEdge outside_edge = new PAEdge(null,  "", null);

    /** Applies rule 22: matches an outside edge starting from node1 in 
	pig[i] against an inside edge from pig[i] (the same analysis scope).
	To preserve some precision, rule 22 consider only inside edges that
	could be created before the outside edge is read (the edge ordering
	info helps us a lot).<br>
	Some (new) mappings for node2 will be generated and put in mu[i].
	This rule is designed to be used in an incremental manner: it works
	only with the new mappings of node1. All the new mappings for node2
	will also be put in new_info[i]. If such new mappings appear, 
	all the instances of node2 are added to W[i].*/
    public static final void rule22(PANode node1, Set new_mappings_for_node1,
		   ParIntGraph pig[],
		   PAWorkList W[], Relation mu[], Relation new_info[], 
		   int i, int ib){
	// nodes3 stands for all the new instances of n3
	// from the inference rule
	Set nodes3 = new_mappings_for_node1;

	outside_edge.n1 = node1;

	Enumeration flags = pig[i].G.O.allFlagsForNode(node1);
	while(flags.hasMoreElements()){
	    String f = (String) flags.nextElement();
	    
	    // nodes2 stands for all the nodes that could play
	    // the role of n2 from the inference rule
	    Set nodes2 = pig[i].G.O.pointedNodes(node1,f);
	    if(nodes2.isEmpty()) continue;
	    
	    outside_edge.f = f;
	    Iterator it2 = nodes2.iterator();
	    while(it2.hasNext()){
		PANode node2 = (PANode) it2.next();
		outside_edge.n2 = node2;

		boolean changed = false;

		// analyze the inside edges that are already created when
		// outside edge is read.
		Iterator it_edges = pig[i].eo.getBeforeEdges(outside_edge);
		while(it_edges.hasNext()){
		    PAEdge inside_edge = (PAEdge) it_edges.next();
		    // only edges started in one of the possible node3 nodes
		    if(!nodes3.contains(inside_edge.n1)) continue;
		    PANode node4 = inside_edge.n2;
		    if(mu[i].add(node2,node4)){
			changed = true;
			new_info[i].add(node2,node4);
		    }
		}
		// nodes with new info are put in the worklist
		if(changed) W[i].add(node2);
	    }
	}
    }


    /** Applies rule 3: matches an inside edge starting from node1 in 
        pig[i] against an outside edge in pig[ib]. As a result, some possibly
        new mappings for node4 will be generated and put in mu[ib].
        This rule is designed to be used in an incremental manner: it works
        only with the new mappings of node1. All the new mappings for node4
        will also be put in new_info[ib]. If such new mappings appear, 
        all the instances of node4 are added to W[ib].*/
    public static final void rule3(PANode node1, Set new_mappings_for_node1,
                   ParIntGraph pig[],
                   PAWorkList W[], Relation mu[], Relation new_info[], 
                   int i, int ib){
        // nodes3 stands for all the new instances of n3
        // from the inference rule
        Set nodes3 = new_mappings_for_node1;

        Enumeration flags = pig[i].G.I.allFlagsForNode(node1);
        while(flags.hasMoreElements()){
            String f = (String) flags.nextElement();
            
            // nodes2 stands for all the nodes that could play
            // the role of n2 from the inference rule
            Set nodes2 = pig[i].G.I.pointedNodes(node1,f);
            if(nodes2.isEmpty()) continue;
            
            // nodes4 stands for all the nodes that could play
            // the role of n4 from the inference rule
            Set nodes4 = pig[ib].G.O.pointedNodes(nodes3,f);
            if(nodes4.isEmpty()) continue;

            // set up the relation mu[ib] from any node from nodes4
            // to any node from nodes2
            Iterator it4 = nodes4.iterator();
            while(it4.hasNext()){
                PANode node4 = (PANode) it4.next();
                boolean changed = false;
                Iterator it2 = nodes2.iterator();
                while(it2.hasNext()){
                    PANode node2 = (PANode) it2.next();
                    if(mu[ib].add(node4,node2)){
                        changed = true;
                        new_info[ib].add(node4,node2);
                    }
                }
                // nodes with new info are put in the worklist
                if(changed) W[ib].add(node4);
            }
        }       
    }


    /** Applies rule 32: matches an inside edge starting from node1 in 
	pig[i] against an outside edge in pig[i] (the same analysis scope). 
	To preserve some precision, rule 32 consider only inside edges that
	could be created before the outside edge is read (the edge ordering
	info helps us a lot).<br>
	Some (new) mappings for node2 will be generated and put in mu[i].
	This rule is designed to be used in an incremental manner: it works
	only with the new mappings of node1. All the new mappings for node2
	will also be put in new_info[i]. If such new mappings appear, 
	all the instances of node2 are added to W[i].*/
    public static final void rule32(PANode node1, Set new_mappings_for_node1,
		   ParIntGraph pig[],
		   PAWorkList W[], Relation mu[], Relation new_info[], 
		   int i, int ib){
	// nodes3 stands for all the new instances of n3
	// from the inference rule
	Set nodes3 = new_mappings_for_node1;

	inside_edge.n1 = node1;

	Enumeration flags = pig[i].G.I.allFlagsForNode(node1);
	while(flags.hasMoreElements()){
	    String f = (String) flags.nextElement();
	    
	    inside_edge.f  = f;
	    outside_edge.f = f;

	    // nodes2 stands for all the nodes that could play
	    // the role of n2 from the inference rule
	    Set nodes2 = pig[i].G.I.pointedNodes(node1,f);
	    if(nodes2.isEmpty()) continue;
	    
	    Iterator it3 = nodes3.iterator();
	    while(it3.hasNext()){
		PANode node3 = (PANode) it3.next();
		outside_edge.n1 = node3;

		Iterator it4 = pig[i].G.O.pointedNodes(node3,f).iterator();
		while(it4.hasNext()){
		    PANode node4 = (PANode) it4.next();
		    outside_edge.n2 = node4;
		    
		    Iterator it2 = nodes2.iterator();
		    while(it2.hasNext()){
			PANode node2 = (PANode) it2.next();
			inside_edge.n2 = node2;

			if(pig[i].eo.wasBefore(inside_edge, outside_edge))
			    if(mu[ib].add(node4,node2)){
				new_info[ib].add(node4,node2);
				W[i].add(node4);
			    }
		    }
		}
	    }
	}
    }

}



