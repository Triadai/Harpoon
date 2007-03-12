// AbstrRelationMapBased.java, created Fri Jun 30 11:17:10 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.PointerAnalysis;

import java.util.Set;
import java.util.Map;
import java.util.Collections;

import java.io.Serializable;


/**
 * <code>AbstrRelationMapBased</code>
 * 
 * @author  Alexandru SALCIANU <salcianu@retezat.lcs.mit.edu>
 * @version $Id: AbstrRelationMapBased.java,v 1.1 2005-08-17 23:34:00 salcianu Exp $
 */
public abstract class AbstrRelationMapBased extends AbstrRelation
    implements Serializable {
    
    // A map from keys to sets of values.
    protected Map map = null;
    

    public void removeKey(Object key) {
	hashCode = 0;
	map.remove(key);
    }

    
    public Set getValues(Object key) {
	Set retval = (Set) getValues2(key);
	if(retval == null)
	    retval = Collections.EMPTY_SET;
	return retval;
    }

    protected Set getValues2(Object key) {	
	return (Set) map.get(key);
    }


    public Set keys() {
	return map.keySet();
    }
    
}