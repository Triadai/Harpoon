// MaxMunchCG.java, created Fri Feb 11 01:26:47 2000 by pnkfelix
// Copyright (C) 2000 Felix S. Klock <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Backend.Generic;

import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCodeElement;
import harpoon.IR.Assem.Instr;
import harpoon.Temp.Temp;
import harpoon.Analysis.Maps.Derivation;

import harpoon.Util.Util;
import harpoon.Util.Default;

import java.util.Map;
import java.util.HashMap;

/**
 * <code>MaxMunchCG</code> is a <code>MaximalMunchCGG</code> specific 
 * extension of <code>CodeGen</code>.  Its purpose is to incorporate
 * functionality common to all target architectures but specific to
 * the particular code generation strategy employed by the CGG.  Other
 * <code>CodeGeneratorGenerator</code> implementations should add
 * their own extensions of <code>CodeGen</code>.
 * 
 * @author  Felix S. Klock <pnkfelix@mit.edu>
 * @version $Id: MaxMunchCG.java,v 1.1.2.5 2000-02-18 01:17:55 pnkfelix Exp $ */
public abstract class MaxMunchCG extends CodeGen {
    
    /** Creates a <code>MaxMunchCG</code>. */
    public MaxMunchCG(Frame frame) {
        super(frame);
    }

    // first = null OR first instr passed to emit(Instr)
    protected Instr first;

    // last = null OR last instr passed to emit(Instr)
    protected Instr last; 
    
    /** Emits <code>i</code> as the next instruction in the
        instruction stream.
    */	
    protected Instr emit(Instr i) {
	debug( "Emitting "+i.toString() );
	if (first == null) {
	    first = i;
	}
	// its correct that last==null the first time this is called
	i.layout(last, null);
	last = i;

	java.util.Iterator defs = i.defC().iterator();
	while(defs.hasNext()) {
	    Temp t = (Temp) defs.next();
	    TypeAndDerivation td = 
		(TypeAndDerivation) tempToType.get(t);
	    Util.assert(td != null, 
			"Uh oh forgot to declare "+t+" before "+i);
	    ti2td.put(Default.pair(t, i), td);
	}

	return i;
    }

    public Derivation getDerivation() {
	return new Derivation() {
	    public Derivation.DList derivation(HCodeElement hce, Temp t) 
		throws TypeNotKnownException {
		return 
		    ((TypeAndDerivation) 
		     ti2td.get( Default.pair(hce, t) )).dlist;
	    }
	    
	    public HClass typeMap(HCodeElement hce, Temp t) 
		throws TypeNotKnownException {
		return 
		    ((TypeAndDerivation) 
		     ti2td.get( Default.pair(hce, t) )).type;
	    }
	};
    }

    // tXi -> TypeAndDerivation
    private Map ti2td = new HashMap();

    private Map tempToType = new HashMap();
	
    public void declare(Temp t, HClass clz) {
	// System.out.println(t + " " + clz);
	tempToType.put(t, new TypeAndDerivation(clz));
    }
    
    public void declare(Temp t, Derivation.DList dl) {
	// System.out.println(t + " " + dl);
	tempToType.put(t, new TypeAndDerivation(dl));
    }
    
    
    // union type for Derivation.DList and HClass
    static class TypeAndDerivation {
	Derivation.DList dlist;
	HClass type;
	TypeAndDerivation(HClass hc) {
	    type = hc;
	}
	TypeAndDerivation(Derivation.DList dl) {
	    dlist = dl;
	}
    }

    
}
