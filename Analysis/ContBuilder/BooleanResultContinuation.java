// BooleanResultContinuation.java, created Fri Nov  5 14:34:24 1999 by kkz
// Copyright (C) 1999 Karen K. Zee <kkzee@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.Analysis.ContBuilder;

/**
 * <code>BooleanResultContinuation</code>
 * 
 * @author Karen K. Zee <kkzee@alum.mit.edu>
 * @version $Id: BooleanResultContinuation.java,v 1.1.2.2 1999-11-12 05:18:37 kkz Exp $
 */
public interface BooleanResultContinuation extends Continuation {

    public void resume(boolean result);
}
