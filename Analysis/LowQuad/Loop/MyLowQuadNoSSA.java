// MyLowQuadNoSSA.java, created Fri Jul  9 14:13:48 1999 by root
// Copyright (C) 1999 Brian Demsky <bdemsky@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.LowQuad.Loop;

import harpoon.Analysis.Maps.Derivation;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HMethod;
import harpoon.IR.LowQuad.Code;
import harpoon.IR.LowQuad.LowQuadSSI;
import harpoon.IR.Quads.Quad;
import harpoon.IR.Quads.RSSxToNoSSA;
import harpoon.Temp.Temp;
import harpoon.Temp.TempMap;
import harpoon.Util.Util;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * <code>MyLowQuadNoSSA</code>
 * 
 * @author  Brian Demsky <bdemsky@mit.edu>
 * @version $Id: MyLowQuadNoSSA.java,v 1.1.2.4 2000-11-13 21:06:29 cananian Exp $
 */

public class MyLowQuadNoSSA extends harpoon.IR.LowQuad.LowQuadNoSSA
    implements Derivation 
    /* ergh.  implementing Derivation directly is bad.  bdemsky should fix. */
{
    HashMap dT;
    HashMap tT;
    public static final String codename = "low-quad-ssa";
    Derivation parentDerivation;
    Map quadmap;
    TempMap tempMap;
    
    MyLowQuadNoSSA(final LowQuadSSI code) {
	super(code.getMethod(),null);
	RSSxToNoSSA translate=new RSSxToNoSSA(qf, code);
	quads=translate.getQuads();
	tempMap=translate.tempMap();
	quadmap=translate.quadMap();
	dT=new HashMap();
	tT=new HashMap();
	parentDerivation=code.getDerivation();
	buildmaps(code);
	setDerivation(this);
    }

    private void buildmaps(final HCode code) {
	Iterator iterate=((HCode)code).getElementsI();
	while (iterate.hasNext()) {
	    Quad q=(Quad)iterate.next();
	    Temp[] defs=q.def();
	    for(int i=0;i<defs.length;i++) {
		Derivation.DList parents=parentDerivation.derivation(q, defs[i]);
		if (parents!=null) {
		    dT.put(tempMap.tempMap(defs[i]),Derivation.DList.rename(parents,tempMap));
		    tT.put(tempMap.tempMap(defs[i]), 
			   new Error("Cant type derived pointer: "+tempMap.tempMap(defs[i])));
		} else
		    tT.put(tempMap.tempMap(defs[i]),parentDerivation.typeMap(null,defs[i]));
	    }
	}
    }
    
    public Derivation.DList derivation(HCodeElement hce, Temp t) {
	Util.assert(hce!=null && t!=null);
	return (Derivation.DList)dT.get(t);
    }
    

    public HClass typeMap(HCodeElement hce, Temp t) {
	Util.assert(t!=null);
	Object type = tT.get(t);
	try { return (HClass)type; } 
	catch (ClassCastException cce) { 
	    throw (Error)((Error)type).fillInStackTrace();
	}
    }

    public String getName() {
	return codename;
    }
}