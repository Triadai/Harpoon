// Data.java, created Wed Sep  8 15:45:19 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.Backend.Generic.Frame;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCode.PrintCallback;
import harpoon.ClassFile.HData;
import harpoon.ClassFile.HDataElement;
import harpoon.Temp.TempFactory;

import java.util.Iterator;
import java.util.List;
/**
 * <code>Data</code> is an abstract implementation of <code>HData</code>
 * for <code>IR.Tree</code> form.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Data.java,v 1.4 2002-08-31 00:24:58 cananian Exp $
 */
public abstract class Data extends harpoon.ClassFile.HData {
    protected final String desc;
    protected final Frame frame;
    protected final TreeFactory tf;

    /** Keep track of modifications to this <code>Data</code> so that the
     *  <code>getElementsI()</code> <code>Iterator</code> can fail-fast. */
    protected int modCount=0;

    /** Create a proper TreeFactory. */
    protected class TreeFactory extends harpoon.IR.Tree.TreeFactory {
	private int id=0;
	TreeFactory() { }
	/** No temp factory for Data */
	public TempFactory tempFactory() { return null; }
	/** Returns the <code>HCode</code> to which all
	 *  <code>HDataElement</code>s generated by this factory belong. */
	public Data getParent() { return Data.this; }
	/** Indicate that the parent has changed, so that its
	 *  fail-fast iterators will work correctly. */
	void incModCount() { Data.this.modCount++; }
	/** Returns the <code>HClass</code> to which all
	 *  <code>HDataElement</code>s generated by this factory belong. */
	public Frame getFrame() { return Data.this.frame; } 
	synchronized int getUniqueID() { return id++; }
	public String toString() { 
	    return "Data.TreeFactory["+getParent().toString()+"]"; 
	}
	public int hashCode() { return Data.this.hashCode(); }
    }
    protected Data(String desc, Frame f) {
	this.desc = desc;
	this.frame = f;
	this.tf = new TreeFactory();
    }
    public String getDesc() { return desc; }

    /** Print a human-readable representation of this dataview */
    public void print(java.io.PrintWriter pw, PrintCallback callback) {
	Print.print(pw, this, callback);
    }
}
