// FakeMetaCallGraph.java, created Mon Mar 13 15:56:57 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.MetaMethods;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import harpoon.Analysis.Quads.CallGraph;
import harpoon.ClassFile.HMethod;
import harpoon.IR.Quads.CALL;

/**
 * <code>FakeMetaCallGraph</code> converts a classic <code>CallGraph</code> to
 a <code>MetaCallGraph</code>. Basically, it offers a <i>MetaMethods</i>-view
 of the call graph, without doing any specialization. This is possible because
 there is an obvious mapping from each method of the program to an
 unspecialized meta-method consisting of that method plus its declared
 parameter types (marked as polymorphic to cope with all the possible cases
 where that method could be called).

 Although it doesn't offer any additional interface,
 <code>FakeMetaCallGraph</code> allows the use of the components that require
 &quot;meta methods&quot; even without specializing methods to meta-methods.
 For example, if you need to use the <code>PointerAnalysis</code> stuff, but
 don't feel very comfortable with meta-methods, just pass it a 
 <code>FakeMetaCallGraph</code>.
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: FakeMetaCallGraph.java,v 1.1.2.4 2000-06-27 04:36:10 salcianu Exp $
 */
public class FakeMetaCallGraph extends MetaCallGraphAbstr {

    /** Creates a <code>FakeMetaCallGraph</code>. Receives as parameters
      a <code>CallGraph</code> object and the set of all the executable
      methods of the program. The <code>methods</code> parameter can be
      obtained by using the <code>callableMethods</code> method of 
      <code>ClassHierarchy</code>.
      The resulting <code>FakeMetaCallGraph</code> object is just a
      <code>MetaMethod</code> view of the data contained into <code>cg</code>.
    */
    public FakeMetaCallGraph(CallGraph cg, Set methods, Set runs) {
	Map map = create_map(methods);
	translate(cg,map);

	// initialize the set of "run" methods.
	if(runs != null)
	    for(Iterator it = runs.iterator(); it.hasNext(); ){
		MetaMethod mm = (MetaMethod) it.next();
		run_mms.add(new MetaMethod(mm.getHMethod(), true));
	    }
    }

    // Create the HMethod -> MetaMethod one-to-one map.
    private Map create_map(Set methods){
	Map map = new HashMap();
	for(Iterator it =  methods.iterator(); it.hasNext(); ){
	    HMethod hm = (HMethod) it.next();
	    MetaMethod mm = new MetaMethod(hm);
	    map.put(hm,mm);
	}
	return map;
    }

    // Translate the info from the CallGraph into this MetaCallGraph using map
    private void translate(CallGraph cg, Map map){

	// set the all_meta_method set
	for(Iterator it = map.keySet().iterator(); it.hasNext(); ){
	    HMethod hm = (HMethod) it.next();
	    MetaMethod mm = (MetaMethod) map.get(hm);
	    all_meta_methods.add(mm);
	}
	
	// fill the callees1_cmpct map
	for(Iterator it = map.keySet().iterator(); it.hasNext(); ){
	    HMethod hm = (HMethod) it.next();
	    MetaMethod mm = (MetaMethod) map.get(hm);
	    MetaMethod[] mmc = hms2mms(cg.calls(hm),map);
	    callees1_cmpct.put(mm,mmc);
	}

	// fill the callees2_cmpct map
	for(Iterator it = map.keySet().iterator(); it.hasNext(); ){
	    HMethod hm = (HMethod) it.next();
	    MetaMethod mm = (MetaMethod) map.get(hm);
	    CALL[] css = cg.getCallSites(hm);
	    for(int i = 0; i < css.length; i++){
		CALL cs = css[i];
		MetaMethod[] mmc = hms2mms(cg.calls(hm,cs),map);
		Map cs2mm = (Map) callees2_cmpct.get(mm);
		if(cs2mm == null)
		    callees2_cmpct.put(mm, cs2mm = new HashMap());
		cs2mm.put(cs,mmc);
	    }
	}
    }

    // convert an array of HMethod's into an array of MetaMethod,
    // according to map.
    private MetaMethod[] hms2mms(HMethod[] hms, Map map){
	MetaMethod[] mms = new MetaMethod[hms.length];
	for(int i = 0; i < hms.length; i++)
	    mms[i] = (MetaMethod) map.get(hms[i]);
	return mms;
    }
}
