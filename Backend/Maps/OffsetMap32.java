// OffsetMap32.java, created Wed Feb  3 18:43:47 1999 by duncan
// Copyright (C) 1998 Duncan Bryce <duncan@lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Backend.Maps;

import harpoon.Analysis.InterfaceMethodMap;
import harpoon.Analysis.QuadSSA.ClassHierarchy;
import harpoon.Backend.Analysis.DisplayInfo.HClassInfo;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HField;
import harpoon.ClassFile.HMethod;
import harpoon.Temp.Label;
import harpoon.Util.Util;

import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * The OffsetMap32 class implements the abstract methods of OffsetMap,
 * specializing them for 32-bit architectures.
 *
 * @author   Duncan Bryce <duncan@lcs.mit.edu>
 * @version  $Id: OffsetMap32.java,v 1.1.2.17 1999-08-11 03:51:48 cananian Exp $
 */
public class OffsetMap32 extends OffsetMap
{
    private static int WORDSIZE = 4;

    private ClassDepthMap       m_cdm;   
    private FieldMap            m_fm;     
    private Hashtable           m_fields; // Cache of field-orderings
    private Hashtable           m_labels; // Cache of label mappings
    private HClassInfo          m_hci;
    private InterfaceMethodMap  m_imm; 
    private MethodMap           m_cmm;
    private NameMap             m_nm;

    /** Class constructor */
    public OffsetMap32(ClassHierarchy ch, NameMap nm) {
	// Util.assert(ch!=null);
    
	m_cmm     = new MethodMap() {
	    public int methodOrder(HMethod hm) { 
		return m_hci.getMethodOffset(hm); 
	    }
	};
	m_cdm     = new ClassDepthMap() {
	    public int classDepth(HClass hc) { return m_hci.depth(hc); }
	    public int maxDepth() { throw new Error("Not impl:  maxDepth()"); }
	};
	m_fm      = new FieldMap() {
	    public int fieldOrder(HField hf) { 
		return m_hci.getFieldOffset(hf); 
	    }
	};
	m_fields  = new Hashtable();
	m_labels  = new Hashtable();
	m_hci     = new HClassInfo();
	// m_imm     = new InterfaceMethodMap(ch.classes());
	m_nm      = nm;
    }
    /** Create an <code>OffsetMap32</code> using a
     *  <code>DefaultNameMap</code>. */
    public OffsetMap32(ClassHierarchy ch) {
	this(ch, new DefaultNameMap());
    }

    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     *                                                           *
     *              Implementation of type tags                  *
     *                                                           *
     *++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


    public int arrayTag()     { return 0; }
    public int classTag()     { return 1; }
    public int interfaceTag() { return 2; }
    public int primitiveTag() { return 3; }


    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     *                                                           *
     *            Implementation of label mappings               *
     *                                                           *
     *++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


    /** Returns the label corresponding to the specified HClass */
    public Label label(HClass hc) { 
	if (!m_labels.containsKey(hc)) {
	    m_labels.put(hc, new Label(m_nm.mangle(hc)));
	}
	return (Label)m_labels.get(hc);
    }
	    
    /** Returns the label corresponding to the specified static field */
    public Label label(HField hf) { 
	Util.assert(hf.isStatic());
	if (!m_labels.containsKey(hf)) {
	    m_labels.put(hf, new Label(m_nm.mangle(hf)));
	}
	return (Label)m_labels.get(hf);
    }

    /** Returns the label corrensponding to the specified method.  This
     *  method is not necessarily static */
    public Label label(HMethod hm) { 
	if (!m_labels.containsKey(hm)) {
	    m_labels.put(hm, new Label(m_nm.mangle(hm))); 
	}
	return (Label)m_labels.get(hm);
    }

    /** Returns the label corresponding to the specified String constant */
    public Label label(String stringConstant) { 
	if (!m_labels.containsKey(stringConstant)) {
	    m_labels.put(stringConstant,
			 new Label(m_nm.mangle(stringConstant)));
	}
	return (Label)m_labels.get(stringConstant);
    }

    
    /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     *                                                           *
     *            Implementation of offset methods               *
     *                                                           *
     *++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


    /** Returns the offset of the class pointer */
    public int classOffset(HClass hc)   { 
	Util.assert(!hc.isPrimitive());
	return -1 * WORDSIZE;
    }

    /** If hc is an array type, returns the offset of its component
     *  type's class pointer */
    public int componentTypeOffset(HClass hc) { 
	Util.assert(hc.isArray());
	return -1 * WORDSIZE;
    }

    /** Returns the offset from the class pointer of this class's pointer
     *  in the display */
    public int displayOffset(HClass hc) { 
	Util.assert(!hc.isPrimitive() && !hc.isInterface());
	return m_cdm.classDepth(hc) * WORDSIZE;
    }

    /** Returns the size of the display information of the specified class */
    public int displaySize(HClass hc) {
	Util.assert(!hc.isPrimitive() && !hc.isInterface());
	
	// Arrays always extend _only_ Object
	return hc.isArray()?2*WORDSIZE:64*WORDSIZE;
    }

    /** Returns the offset of the first array element if hc is an array
     *  type, otherwise generates an assertion failure */
    public int elementsOffset(HClass hc) { 
	Util.assert(hc.isArray());
	return 0; 
    }

    /** Returns the offset of the first field in an object of the
     *  specified type */
    public int fieldsOffset(HClass hc) { 
	Util.assert((!hc.isPrimitive()) && (!hc.isArray()));
	return 0;
    }

    /** Returns the offset of the hashcode of the specified object */
    public int hashCodeOffset(HClass hc) { 
	Util.assert(!hc.isPrimitive());
	return -2 * WORDSIZE; 
    }

    /** If hc is a class type, or an interface, returns the offset from
     *  the class pointer of the pointer to implemented interfaces */
    public int interfaceListOffset(HClass hc) { 
	Util.assert(!hc.isPrimitive() && !hc.isArray());
	return -3 * WORDSIZE;
    }

    /** If hc is an array type, returns the offset of its length field */
    public int lengthOffset(HClass hc) { 
	Util.assert(hc.isArray());
	return -3 * WORDSIZE; 
    }

    /** Returns the offset from the object reference of the specified 
     *  non-static field */
    public int offset(HField hf) {
	Util.assert(!hf.isStatic());

	HClass    hc;
	HField[]  fields, orderedFields;
	int       fieldOrder, offset;
    
	hc = hf.getDeclaringClass();
	if (!m_fields.containsKey(hc)) {
	    fields         = hc.getDeclaredFields();
	    orderedFields  = new HField[fields.length];
	    for (int i=0; i<fields.length; i++) 
		orderedFields[m_fm.fieldOrder(fields[i])] = fields[i];
	    // Cache field ordering
	    m_fields.put(hc, orderedFields);
	}
	else 
	    orderedFields  = (HField[])m_fields.get(hc);

	fieldOrder = m_fm.fieldOrder(hf);
	offset     = fieldsOffset(hc);
    
	for (int i=0; i<fieldOrder; i++)
	    offset += size(orderedFields[i].getType(), false); // no inlining

	return offset;
    }


    /** Returns the offset from the class pointer of the specified
     *  non-static method */
    public int offset(HMethod hm) { 
	Util.assert(!hm.isStatic());
	HClass hc = hm.getDeclaringClass(); 
    
	if (hc.isInterface()) return (-m_imm.methodOrder(hm) - 4) * WORDSIZE;
	else return (m_cmm.methodOrder(hm) + displaySize(hc)) * WORDSIZE;
    }

    /** Returns the size of the specified class */
    public int size(HClass hc) {
	return size(hc, true);
    }
  
    /** Returns the size of the specified class */
    private int size(HClass hc, boolean inline) { 
	int size;

	if (hc.isPrimitive()) { 
	    if ((hc==HClass.Long)||(hc==HClass.Double)) size = 2 * WORDSIZE;
	    else size = WORDSIZE;
	}
	else if (hc.isArray()) { 
	    Util.assert(false, "Size of array does not depend on its class!");
	    return -1;
	} 
	else { 
	    if (inline) {
		size = 2 * WORDSIZE;  // Includes hashcode & classptr
	    
		HField[] hf = hc.getDeclaredFields();
		for (int i=0; i<hf.length; i++) {
		    size += hf[i].isStatic()?0:size(hf[i].getType(), false);
		}
	    }
	    else { size = WORDSIZE; }
	}
	
	return size; 
    }

    /** Returns the offset from the class pointer of the tag 
     *  specifying the type of data hc is */
    public int tagOffset(HClass hc) { 
	return -2;
    }
}
