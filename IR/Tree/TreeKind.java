// TreeKind.java, created Mon Jun 28 14:46:11 1999 by duncan
// Copyright (C) 1998 Duncan Bryce <duncan@lcs.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package harpoon.IR.Tree;

/**
 * <code>TreeKind</code> is an enumerated type for the various kinds of
 * <code>Tree</code>s.  Largely copied from Scott's <code>QuadKind</code>
 * class. 
 * 
 * @author  Duncan Bryce <duncan@lcs.mit.edu>
 * $Id: TreeKind.java,v 1.1.2.7 1999-08-05 15:17:14 cananian Exp $
 */
public abstract class TreeKind  {
    public final static int BINOP      = 0;
    public final static int CALL       = 1;
    public final static int CJUMP      = 2;
    public final static int CONST      = 3;
    public final static int DATA       = 4;
    public final static int ESEQ       = 5;
    public final static int EXP        = 6;
    public final static int JUMP       = 7;
    public final static int LABEL      = 8;
    public final static int MEM        = 9;
    public final static int METHOD     = 10;
    public final static int MOVE       = 11;
    public final static int NAME       = 12;
    public final static int NATIVECALL = 13;
    public final static int RETURN     = 14;
    public final static int SEGMENT    = 15;
    public final static int SEQ        = 16;
    public final static int TEMP       = 17;
    public final static int THROW      = 18;
    public final static int UNOP       = 19;

    public static int min() { return 0; }
    public static int max() { return 20; }

    public static boolean isValid(int k) {
	return (min()<=k) && (k<max());
    }
}




