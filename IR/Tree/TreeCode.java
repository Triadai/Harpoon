// TreeCode.java, created Mon Feb 15 12:53:14 1999 by duncan
// Copyright (C) 1998 Duncan Bryce <duncan@lcs.mit.edu> 
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

import harpoon.Analysis.Maps.Derivation;
import harpoon.Analysis.Maps.Derivation.DList;
import harpoon.Analysis.Maps.TypeMap;
import harpoon.Backend.Generic.Frame;
import harpoon.ClassFile.HClass;
import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;
import harpoon.ClassFile.HCodeFactory;
import harpoon.ClassFile.HMethod;
import harpoon.IR.LowQuad.LowQuadNoSSA;
import harpoon.IR.LowQuad.LowQuadSSA;
import harpoon.Temp.CloningTempMap;
import harpoon.Temp.Temp;
import harpoon.Util.Util;

/*
 * The <code>TreeCode</code> codeview exposes a tree-based representation.
 * This codeview serves primarily as an intermediate translation phase between 
 * <code>LowQuadNoSSA</code> and <code>CanonicalTreeCode</code>.  
 * In general, most analyses will be simpler in <code>CanonicalTreeCode</code>
 * because it has no <code>ESEQ</code>s.  
 * 
 * The tree form is based around Andrew Appel's tree form.  
 *
 * @author  Duncan Bryce <duncan@lcs.mit.edu> 
 * @version $Id: TreeCode.java,v 1.1.2.28 2000-02-15 15:49:53 cananian Exp $
 * 
 */
public class TreeCode extends Code {
    public  static   final   String     codename = "tree";
    private          TreeDerivation     treeDerivation;
  
    /** Create a new <code>TreeCode</code> from a
     *  <code>LowQuadNoSSA</code> object, and a <code>Frame</code>.
     */
    TreeCode(LowQuadNoSSA code, Frame topframe) {
	super(code.getMethod(), null, topframe);

	ToTree translator;

	translator = new ToTree(this.tf, code);
	tree       = translator.getTree();
	treeDerivation = translator.getTreeDerivation();
    }
    /** Create a new <code>TreeCode</code> from a
     *  <code>LowQuadSSA</code> object, and a <code>Frame</code>.
     */
    TreeCode(LowQuadSSA code, Frame topframe) {
	super(code.getMethod(), null, topframe);

	ToTree translator;

	translator = new ToTree(this.tf, code);
	tree       = translator.getTree();
	treeDerivation = translator.getTreeDerivation();
    }

    protected TreeCode(HMethod newMethod, Tree tree, Frame topframe,
		       TreeDerivation treeDerivation) {
	super(newMethod, tree, topframe);
	this.treeDerivation = treeDerivation;
    }

    public TreeDerivation getTreeDerivation() { return treeDerivation; }

    /** 
     * Clone this code representation. The clone has its own
     * copy of the tree structure. 
     */
    public HCode clone(HMethod newMethod, Frame frame) {
	TreeCode             tc  = new TreeCode(newMethod, null, frame, null); 

	tc.tree = (Tree)(Tree.clone(tc.tf, tree, null));
	tc.treeDerivation = null; // XXX THIS IS BROKEN.

	return tc;
    }

    /**
     * Return the name of this code view.
     * @return the string <code>"tree"</code>.
     */
    public String getName() { return codename; }

    /** @return false */
    public boolean isCanonical() { return false; } 

    /** 
     * This operation is not supported in non-canonical tree forms.
     * @exception UnsupportedOperationException always.
     */
    public void recomputeEdges() { throw new UnsupportedOperationException(); }

    /**
     * Returns a code factory for <code>TreeCode</code>, given a 
     * code factory for <code>LowQuadNoSSA</code> or <code>LowQuadSSA</code>.
     * <BR> <B>effects:</B> if <code>hcf</code> is a code factory for
     *      <code>LowQuadNoSSA</code> or <code>LowQuadSSA</code>, then
     *      creates and returns a code
     *      factory for <code>TreeCode</code>.  Else passes
     *      <code>hcf</code> to
     *      <code>LowQuadSSA.codeFactory()</code>, and reattempts to
     *      create a code factory for <code>TreeCode</code> from the
     *      code factory returned by <code>LowQuadSSA</code>.
     * @see LowQuadSSA#codeFactory(HCodeFactory)
     */
    public static HCodeFactory codeFactory(final HCodeFactory hcf, 
					   final Frame frame) {
	if (hcf.getCodeName().equals(LowQuadSSA.codename)) {
	    // note that result will not be serializable unless frame is.
	    return new harpoon.ClassFile.SerializableCodeFactory() { 
		public HCode convert(HMethod m) { 
		    HCode c = hcf.convert(m);
		    return (c==null) ? null :
			new TreeCode((LowQuadSSA)c, frame);
		}
		public void clear(HMethod m) { hcf.clear(m); }
		public String getCodeName() { return codename; }
	    };
	} else if (hcf.getCodeName().equals(LowQuadNoSSA.codename)) {
	    // note that result will not be serializable unless frame is.
	    return new harpoon.ClassFile.SerializableCodeFactory() { 
		public HCode convert(HMethod m) { 
		    HCode c = hcf.convert(m);
		    return (c==null) ? null :
			new TreeCode((LowQuadNoSSA)c, frame);
		}
		public void clear(HMethod m) { hcf.clear(m); }
		public String getCodeName() { return codename; }
	    };
	} else {
	    //   throw new Error("don't know how to make " + codename +
	    //	    " from " + hcf.getCodeName());
	    HCodeFactory lqnossaHCF = LowQuadSSA.codeFactory(hcf);
	    return codeFactory(lqnossaHCF, frame);
	}
    }
  
    /**
     * Return a code factory for <code>TreeCode</code>, using the default
     * code factory for <code>LowQuadSSA</code>
     */
    public static HCodeFactory codeFactory(final Frame frame) {  
	return codeFactory(LowQuadSSA.codeFactory(), frame);
    }
}
