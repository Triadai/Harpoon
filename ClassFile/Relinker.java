// Relinker.java, created Mon Dec 27 19:05:58 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.ClassFile;

import harpoon.Util.Util;

import java.util.HashMap;
import java.util.Map;
/**
 * A <code>Relinker</code> object is a <code>Linker</code> where one
 * can globally replace references to a certain class with references
 * to another, different, class.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Relinker.java,v 1.1.2.3 2000-01-11 15:31:41 cananian Exp $
 */
public class Relinker extends Linker {
    protected final Linker linker;

    /** Creates a <code>Relinker</code>. */
    public Relinker(Linker linker) {
	this.linker = linker;
    }
    protected HClass forDescriptor0(String descriptor) {
	return new HClassProxy(this, linker.forDescriptor(descriptor));
    }
    
    /** Creates a mutable class with the given name which is based on
     *  the given template class.  The name <b>need not</b> be unique.
     *  If a class with the given name already exists, all references
     *  to the existing class are changed to point to the new mutable
     *  class returned by this method. */
    public HClass createMutableClass(String name, HClass template) {
	try {
	    linker.createMutableClass(name, template);
	    return forName(name); // wrap w/ proxy class.
	} catch (DuplicateClassException e) {
	    HClass newClass = new HClassSyn(this, name, template);
	    HClass oldClass = forName(name); // get existing proxy class
	    if (oldClass.equals(template))
		newClass.hasBeenModified=false; // exact copy of oldClass
	    relink(oldClass, newClass);
	    return oldClass;
	}
    }

    /** Globally replace all references to <code>oldClass</code> with
     *  references to <code>newClass</code>, which may or may not have
     *  the same name.  The following constraint must hold:<pre>
     *  oldClass.getLinker()==newClass.getLinker()==this
     *  </pre><p>
     *  <b>WARNING:</b> the <code>hasBeenModified()</code> method of
     *  <code>HClass</code>is not reliable after calling 
     *  <code>relink()</code> if <code>oldClass.getName()</code> is not the
     *  same as <code>newClass.getName()</code>.  The value returned
     *  by <code>HClass.hasBeenModified()</code> will not reflect changes
     *  due to the global replacement of <code>oldClass</code> with
     *  <code>newClass</code> done by this <code>relink()</code>.</p>
     */
    public void relink(HClass oldClass, HClass newClass) {
	Util.assert(oldClass.getLinker()==this);
	Util.assert(newClass.getLinker()==this);
	// we're going to leave the old mapping in, so that classes
	// loaded in the future still get the new class.  uncomment
	// out the next line if we decide to delete the old descriptor
	// mapping when we relink.
	//descCache.remove(oldClass.getDescriptor());
	((HClassProxy)oldClass).relink(newClass);
	descCache.put(oldClass.getDescriptor(), oldClass);
    }

    // WRAP/UNWRAP CODE
    Map memberMap = new HashMap();

    HClass wrap(HClass hc) {
	return forDescriptor(hc.getDescriptor());
    }
    HClass unwrap(HClass hc) {
	if (hc==null || hc.isPrimitive()) return hc;
	return ((HClassProxy)hc).proxy;
    }
    HField wrap(HField hf) {
	HField result = (HFieldProxy) memberMap.get(hf);
	if (result==null) {
	    result = new HFieldProxy(this, hf);
	    memberMap.put(hf, result);
	}
	return result;
    }
    HMethod wrap(HMethod hm) {
	if (hm instanceof HInitializer) return wrap((HInitializer)hm);
	if (hm instanceof HConstructor) return wrap((HConstructor)hm);
	HMethod result = (HMethodProxy) memberMap.get(hm);
	if (result==null) {
	    result = new HMethodProxy(this, hm);
	    memberMap.put(hm, result);
	}
	return result;
    }
    HConstructor wrap(HConstructor hc) {
	HConstructor result = (HConstructorProxy) memberMap.get(hc);
	if (result==null) {
	    result = new HConstructorProxy(this, hc);
	    memberMap.put(hc, result);
	}
	return result;
    }
    HInitializer wrap(HInitializer hi) {
	HInitializer result = (HInitializerProxy) memberMap.get(hi);
	if (result==null) {
	    result = new HInitializerProxy(this, hi);
	    memberMap.put(hi, result);
	}
	return result;
    }    
    // array wrap/unwrap
    HClass[] wrap(HClass hc[]) {
	HClass[] result = new HClass[hc.length];
	for (int i=0; i<result.length; i++)
	    result[i] = wrap(hc[i]);
	return result;
    }
    HClass[] unwrap(HClass[] hc) {
	HClass[] result = new HClass[hc.length];
	for (int i=0; i<result.length; i++)
	    result[i] = unwrap(hc[i]);
	return result;
    }
    HField[] wrap(HField hf[]) {
	HField[] result = new HField[hf.length];
	for (int i=0; i<result.length; i++)
	    result[i] = wrap(hf[i]);
	return result;
    }
    HMethod[] wrap(HMethod hm[]) {
	HMethod[] result = new HMethod[hm.length];
	for (int i=0; i<result.length; i++)
	    result[i] = wrap(hm[i]);
	return result;
    }
    HConstructor[] wrap(HConstructor hc[]) {
	HConstructor[] result = new HConstructor[hc.length];
	for (int i=0; i<result.length; i++)
	    result[i] = wrap(hc[i]);
	return result;
    }
}
