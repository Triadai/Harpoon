// BasicBlockFactoryInterf.java, created Fri Dec 14 19:47:04 2001 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@MIT.EDU>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis;

import java.util.Set;

import harpoon.ClassFile.HCode;
import harpoon.ClassFile.HCodeElement;

/**
 * <code>BasicBlockFactoryInterf</code>
 * 
 * @author  Alexandru SALCIANU <salcianu@MIT.EDU>
 * @version $Id: BasicBlockFactoryInterf.java,v 1.1.2.1 2001-12-16 05:07:00 salcianu Exp $
 */
public interface BasicBlockFactoryInterf {
    
    /** Returns the <code>HCode</code> that <code>this</code> factory
	produces basic blocks of. */
    public HCode getHCode();

    /** Returns the root <code>BasicBlockInterfs</code>.
	<BR> <B>effects:</B> returns the <code>BasicBlock</code>
	that is at the start of the set of
	<code>HCodeElement</code>s being analyzed.
    */
    public BasicBlockInterf getRootBBInterf();

    /** Returns the leaf <code>BasicBlockInterf</code>s.
	<BR> <B>effects:</B> returns a <code>Set</code> of
	<code>BasicBlock</code>s that are at the ends of the
	<code>HCodeElement</code>s being analyzed.
    */
    public Set getLeavesBBInterf();

    /** Returns the <code>BasicBlock</code>s constructed by
	the factory. */
    public Set blockSet();
 
    /** Returns the <code>BasicBlockInterf</code> containing
	<code>hce</code>. 
	<BR> <B>requires:</B> hce is present in the code for
	<code>this</code>. 
	<BR> <B>effects:</B> returns the basic block that contains
	<code>hce</code>, or <code>null</code> if
	<code>hce</code> is unreachable.
    */
    public BasicBlockInterf getBBInterf(HCodeElement hce);
   
}