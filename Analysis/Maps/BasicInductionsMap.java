// BasicInductionsMap.java, created Tue Jun 29 14:13:27 1999 by bdemsky
// Copyright (C) 1999 Brian Demsky <bdemsky@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.Maps;

import harpoon.Analysis.Loops.Loops;
import harpoon.ClassFile.HCode;

import java.util.Set;


/**
 * <code>BasicInductionsMap</code> is a mapping from <code>Loops</code> to a
 * <code>Set</code> of basic induction <code>Temp</code>s.
 *
 * @author  Brian Demsky <bdemsky@mit.edu>
 * @version $Id: BasicInductionsMap.java,v 1.1.2.1 1999-06-29 18:55:56 bdemsky Exp $
 */
public interface BasicInductionsMap {
    /** Returns a <code>Set</code> of basic induction <code>Temp</code>s. */
    public Set basicInductionsMap(HCode hc, Loops lp);
}
