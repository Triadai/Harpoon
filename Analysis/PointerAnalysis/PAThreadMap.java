// PAThreadMap.java, created Sun Jan  9 15:49:32 2000 by salcianu
// Copyright (C) 1999 Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.PointerAnalysis;

import java.lang.System;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>PAThreadMap</code> implements the parallel thread map
 * (i.e. the "tau" function from the algorithm). For each thread node
 * n, tau(n) is a conservative approximation of the number of instances
 * of nT that could run in parallel with the current thread.
 * tau(n) is a number from the set {0,1,2} where 0 stands for no instance,
 * 1 for at most one instance and 2 for possibly multiple instances.
 * 
 * <code>PAThreadMap</code> is more or less a <code>Hashtable</code> with
 * some access functions to enforce the new rules for addition and
 * substraction.
 *
 * @author  Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
 * @version $Id: PAThreadMap.java,v 1.1.2.2 2000-01-16 02:24:32 salcianu Exp $
 */
public class PAThreadMap{

    static private final Integer ONE  = new Integer(1);
    static private final Integer TWO  = new Integer(2);

    private Hashtable hash;
    
    /** Creates a <code>PAThreadMap</code>. */
    public PAThreadMap() {
        hash = new Hashtable();
    }

    // Look at the value attached to the thread node n
    // (i.e. tau(n)
    public int getValue(PANode n){
	Integer v = (Integer)hash.get(n);

	if(v != null){
	    return ((Integer) hash.get(n)).intValue();
	}
	else return 0;
    }

    /** Increments the value attached to <code>n</code> */
    public void inc(PANode n){
	Integer v = (Integer)hash.get(n);

	if(v == null) hash.put(n,ONE);
	if(v == ONE) hash.put(n,TWO);
    }
    
    /** Decrements the value attached to <code>n</code> */
    public void dec(PANode n){
	Integer v = (Integer)hash.get(n);

	if(v==null){
	    System.err.println("Error in PAThreadMap: cannot do 0--\n");
	    System.exit(1);
	}

	// another alternative would be to attach to n a special object
	// ZERO. However, I decided to remove the mapping for n - in this
	// way, we don't create two values for expressing tau(n)=0 (null
	// and ZERO) and decrease the number of keys from the map.
	if(v==ONE) hash.remove(n);
	if(v==TWO) hash.put(n,TWO);
    }

    // Returns all the thread nodes nT such that tau(nT) > 0 
    public Enumeration activeThreads(){
	return hash.keys();
    }

    /** <code>join</code> combines two <code>PAThreadMap</code>s in
     *  a control-flow join poin */
    public void join(PAThreadMap tau2){
	Enumeration e = tau2.activeThreads();
	while(e.hasMoreElements()){
	    PANode n = (PANode) e.nextElement();
	    int count1 = getValue(n);
	    int count2 = tau2.getValue(n);
	    if(count2 > count1)
		if(count2 == 1) hash.put(n,ONE);
		else hash.put(n,TWO);
	}
    }

    /** Private constructor used by <code>clone</code> and  
     * <code>keepTheEssential</code> */
    private PAThreadMap(Hashtable _hash){
	hash = _hash;
    }
    
    /** <code>clone</code> creates a copy of <code>this</code> thread map;
     *	by doing a simple shallow copy of the <code>hash<code> field. 
     */
    public Object clone(){
	return new PAThreadMap((Hashtable)hash.clone());
    }

    /** Produces a new <code>PAThreadMap</code> containing only the thread
     * nodes that appear in <code>essential_nodes</code>, too */
    public PAThreadMap keepTheEssential(Set essential_nodes){
	Hashtable _hash = new Hashtable();
	Iterator it = hash.entrySet().iterator();
	while(it.hasNext()){
	    Map.Entry entry = (Map.Entry)it.next();
	    PANode node = (PANode)entry.getKey();    
	    if(essential_nodes.contains(node))
		_hash.put(node,entry.getValue());
	}
	return new PAThreadMap(_hash);
    }

    /** Pretty print function for debug purposes. */
    public String toString(){
	StringBuffer buffer = new StringBuffer("Parallel Thread Map:\n");
	Iterator it = hash.entrySet().iterator();
	while(it.hasNext()){
	    Map.Entry e = (Map.Entry)it.next();
	    buffer.append("  " + e.getKey().toString() + " -> " + 
			  e.getValue().toString() + "\n");
	}
	return buffer.toString();
    }

}
